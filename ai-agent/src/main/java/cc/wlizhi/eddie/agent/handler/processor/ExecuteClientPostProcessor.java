/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.dao.AgentMsgStepDao;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentStepStreamContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.handler.UnifiedAgentToolInterceptor;
import cc.wlizhi.eddie.agent.service.impl.AgentStepWindowedMemory;
import cc.wlizhi.eddie.agent.tool.StepFinishTool;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
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
import java.util.stream.Collectors;

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
    private OwnerToolBindingContext ownerToolBindingContext;
    @Resource
    private AgentMsgStepDao agentMsgStepDao;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.EXECUTE == agentMode;
    }

    @Override
    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext ctx) {
        long startTime = System.currentTimeMillis();
        String resolvePrompts = agentPromptsResolver.resolvePrompts(ctx);
        log.debug("{} 模式系统提示词：\n{}", AgentMode.EXECUTE.name(), resolvePrompts);

        String stepConversationId = ctx.getAgentMsg().getId() + ":" + ctx.getMetrics().getCurrentStepNumber();

        String userPrompt = "请根据系统提示词及历史消息（如果有）继续完成当前步骤的任务内容";

        // 初始化步骤级流式累加器，设置 prompt（供 ExecuteResponseStreamProcessor 使用）
        AgentStepStreamContext stepCtx = new AgentStepStreamContext();
        stepCtx.setStepNumber(ctx.getMetrics().getCurrentStepNumber());
        stepCtx.setPrompt(userPrompt);
        // 从 taskPlan 同步当前步骤的状态到迭代缓冲
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan != null && taskPlan.getSteps() != null
                && ctx.getMetrics().getCurrentStepNumber() != null && ctx.getMetrics().getCurrentStepNumber() > 0
                && ctx.getMetrics().getCurrentStepNumber() <= taskPlan.getSteps().size()) {
            AgentTaskStep planStep = taskPlan.getSteps().get(ctx.getMetrics().getCurrentStepNumber() - 1);
            stepCtx.setStepStatus(StepStatus.fromValue(planStep.getStatus()));
        }
        ctx.setStepStreamContext(stepCtx);

        // 预创建步骤占位记录，获取 stepRecordId（审批拦截器需要 stepRecordId 定位步骤）
        String stepDesc = ExecuteResponseStreamProcessor.resolveStepDesc(ctx, ctx.getMetrics().getCurrentStepNumber());
        AgentMsgStepEntity placeholder = ExecuteResponseStreamProcessor.buildPlaceholderEntity(ctx, ctx.getMetrics().getCurrentStepNumber(), stepDesc);
        try {
            Long stepRecordId = agentMsgStepDao.insertPlaceholder(placeholder);
            stepCtx.setStepRecordId(stepRecordId);
            log.info("预创建步骤占位记录成功, stepRecordId={}, stepNumber={}", stepRecordId, ctx.getMetrics().getCurrentStepNumber());
        } catch (Exception e) {
            log.warn("预创建步骤占位记录失败, msgId={}, stepNumber={}: {}",
                    ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null,
                    ctx.getMetrics().getCurrentStepNumber(), e.getMessage());
        }

        // 1. 解析用户可配置的工具（WebSearch/WebFetch/Shell/MCP 等）
        ToolCallback[] configurableTools = toolCallbackResolver.resolve(
                RoleType.AGENT.name(), ctx.getAgent().getId()
                , ctx.getOriginalRequest().getToolSelectionMode()
                , ctx.getOriginalRequest().getToolNames());

        // 2. 一步包装：用 UnifiedAgentToolInterceptor 包装所有工具回调
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(
                RoleType.AGENT.name(), ctx.getAgent().getId());
        Map<String, ToolDefinitionEntity> toolDefMap = boundTools.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));
        Map<String, List<ToolBehavior>> behaviorMap = toolCallbackResolver.getBehaviorMap();

        // 3. 收集所有工具（用户可配 + StepFinishTool 内置工具）
        List<ToolCallback> allTools = new ArrayList<>();
        if (configurableTools != null) {
            allTools.addAll(Arrays.asList(configurableTools));
        }
        ToolCallback[] internalTools = ToolCallbacks.from(stepFinishTool);
        allTools.addAll(Arrays.asList(internalTools));

        // 3. 构建 ChatClient（注入记忆窗口 advisor + 工具）
        ChatClient.Builder builder = ctx.getEvent().getChatClient().mutate();
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(agentStepWindowedMemory).build();
        builder.defaultAdvisors(memoryAdvisor);
        if (!allTools.isEmpty()) {
            Object[] wrappers = allTools.stream()
                    .map(t -> {
                        String name = t.getToolDefinition().name();
                        ToolDefinitionEntity def = toolDefMap.get(name);
                        List<ToolBehavior> behaviors = behaviorMap.getOrDefault(name, List.of());
                        McpServerEntity mcpServer = def != null && def.getMcpServerId() != null
                                ? ownerToolBindingContext.getMcpServer(def.getMcpServerId())
                                : null;
                        return new UnifiedAgentToolInterceptor(t, def, behaviors, mcpServer, ctx);
                    })
                    .toArray();
            builder.defaultTools(wrappers);
        }

        ChatClient.ChatClientRequestSpec requestSpec = builder.build().prompt()
                .system(resolvePrompts)
                .user(userPrompt)
                .toolContext(Map.of("agentChatContext", ctx))
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", stepConversationId)
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
        log.debug("[buildChatClientRequestSpec] 构建请求客户端，耗时：{}ms", System.currentTimeMillis() - startTime);
        return requestSpec;
    }
}
