/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentStepStreamContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.handler.AgentToolCallbackWrapper;
import cc.wlizhi.eddie.agent.service.impl.AgentStepWindowedMemory;
import cc.wlizhi.eddie.agent.tool.StepFinishTool;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExecuteClientPostProcessor implements AgentClientPostProcessor {

    @Resource
    private AgentPromptsResolver agentPromptsResolver;
    @Resource
    private AgentStepWindowedMemory agentStepWindowedMemory;
    @Resource
    private ToolCallbackResolver toolCallbackResolver;
    @Resource
    private StepFinishTool stepFinishTool;
    @Resource
    private GlobalConfigContext globalConfigContext;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.EXECUTE == agentMode;
    }

    @Override
    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext ctx) {
        String resolvePrompts = agentPromptsResolver.resolvePrompts(ctx);
        log.debug("{} 模式系统提示词：\n{}", AgentMode.EXECUTE.name(), resolvePrompts);

        String stepConversationId = ctx.getAgentMsg().getId() + ":" + ctx.getCurrentStep();

        String userPrompt = "请根据系统提示词及历史消息（如果有）继续完成当前步骤的任务内容";

        // 初始化步骤级流式累加器，设置 prompt（供 ExecuteResponseStreamProcessor 使用）
        AgentStepStreamContext stepCtx = new AgentStepStreamContext();
        stepCtx.setStep(ctx.getCurrentStep());
        stepCtx.setPrompt(userPrompt);
        // 从 taskPlan 同步当前步骤的状态到迭代缓冲
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan != null && taskPlan.getSteps() != null
                && ctx.getCurrentStep() != null && ctx.getCurrentStep() > 0
                && ctx.getCurrentStep() <= taskPlan.getSteps().size()) {
            AgentTaskStep planStep = taskPlan.getSteps().get(ctx.getCurrentStep() - 1);
            stepCtx.setStepStatus(StepStatus.fromValue(planStep.getStatus()));
        }
        ctx.setStepStreamContext(stepCtx);

        // 1. 解析用户可配置的工具（WebSearch/WebFetch/Shell/MCP 等）
        ToolCallback[] configurableTools = toolCallbackResolver.resolve(
                RoleType.AGENT.name(), ctx.getAgent().getId()
                , ctx.getOriginalRequest().getToolSelectionMode()
                , ctx.getOriginalRequest().getToolNames());

        // 2. 收集所有工具（用户可配 + StepFinishTool 内置工具）
        List<ToolCallback> allTools = new ArrayList<>();
        if (configurableTools != null) {
            allTools.addAll(Arrays.asList(configurableTools));
        }
        ToolCallback[] internalTools = ToolCallbacks.from(stepFinishTool);
        allTools.addAll(Arrays.asList(internalTools));

        // 3. 构建 ChatClient（注入记忆窗口 advisor + 工具）
        ChatClient.Builder builder = ctx.getChatClient().mutate();
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(agentStepWindowedMemory).build();
        builder.defaultAdvisors(memoryAdvisor);
        if (!allTools.isEmpty()) {
            Object[] wrappers = allTools.stream()
                    .map(t -> new AgentToolCallbackWrapper(t, ctx)).toArray();
            builder.defaultTools(wrappers);
        }

        // 4. 解析并注入各层级截断长度
        String modelMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_RESULT_MODEL_MAX_LENGTH);
        int modelMaxLen = ConfigUtil.resolveIntConfig(100000, modelMaxLenStr, 0, 100000);
        ctx.setToolResultModelMaxLength(modelMaxLen);
        String sseMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_CALL_MAX_LENGTH);
        int sseMaxLen = ConfigUtil.resolveIntConfig(5000, sseMaxLenStr, 100, 8000);
        ctx.setToolCallMaxLength(sseMaxLen);
        ctx.setToolCallStoreMaxLength(sseMaxLen >> 4);

        return builder.build().prompt()
                .system(resolvePrompts)
                .user(userPrompt)
                .toolContext(Map.of("agentChatContext", ctx))
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", stepConversationId)
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
    }
}
