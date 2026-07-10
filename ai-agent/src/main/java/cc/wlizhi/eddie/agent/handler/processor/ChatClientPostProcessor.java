package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.handler.UnifiedAgentToolInterceptor;
import cc.wlizhi.eddie.agent.service.impl.AgentShortTermMemory;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.agent.tool.SwitchModeTool;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatClientPostProcessor implements AgentClientPostProcessor {
    @Resource
    private ToolCallbackResolver toolCallbackResolver;
    @Resource
    private AgentPromptsResolver agentPromptsResolver;
    @Resource
    private SwitchModeTool switchModeTool;
    @Resource
    private AgentShortTermMemory agentShortTermMemory;
    @Resource
    private GlobalConfigContext globalConfigContext;
    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;
    @Resource
    private AgentEventPublisher agentEventPublisher;
    @Resource
    private EventRegistry eventRegistry;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.CHAT == agentMode;
    }

    @Override
    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext ctx) {
        // 1. 解析用户可配置的工具
        ToolCallback[] configurableTools = toolCallbackResolver.resolve(
                RoleType.AGENT.name(), ctx.getAgent().getId()
                , ctx.getOriginalRequest().getToolSelectionMode()
                , ctx.getOriginalRequest().getToolNames());

        // 2. 一步包装：用 UnifiedAgentToolInterceptor 包装所有工具回调
        //    内置三层审批决策（助手级 enabled → 行为匹配 → 用户配置），无 instanceof 判断
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(
                RoleType.AGENT.name(), ctx.getAgent().getId());
        Map<String, ToolDefinitionEntity> toolDefMap = boundTools.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));
        Map<String, List<ToolBehavior>> behaviorMap = toolCallbackResolver.getBehaviorMap();

        // 3. 收集所有工具（用户可配 + 智能体内置切换模式工具）
        List<ToolCallback> allTools = new ArrayList<>();
        if (configurableTools != null) {
            allTools.addAll(Arrays.asList(configurableTools));
        }
        ToolCallback[] switchModeTools = ToolCallbacks.from(switchModeTool);
        allTools.addAll(Arrays.asList(switchModeTools));

        // 4. 构建 ChatClient（仅在 chatting 模式注入记忆窗口 advisor）
        ChatClient.Builder builder = ctx.getChatClient().mutate();
        if (ctx.getIteratorState().getAgentMode() == AgentMode.CHAT) {
            var memoryAdvisor = MessageChatMemoryAdvisor.builder(agentShortTermMemory).build();
            builder.defaultAdvisors(memoryAdvisor);
        }
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

        // 4. 解析并注入各层级截断长度
        // 4a. 模型上下文截断（提交给 LLM 的数据）
        String modelMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_RESULT_MODEL_MAX_LENGTH);
        int modelMaxLen = ConfigUtil.resolveIntConfig(100000, modelMaxLenStr, 0, 100000);
        ctx.setToolResultModelMaxLength(modelMaxLen);
        // 4b. SSE 渲染截断（推送前端展示）
        String sseMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_CALL_MAX_LENGTH);
        int sseMaxLen = ConfigUtil.resolveIntConfig(5000, sseMaxLenStr, 100, 8000);
        ctx.setToolCallMaxLength(sseMaxLen);
        // 4c. 存储截断（持久化到数据库，固定为 SSE 截断的一半）
        ctx.setToolCallStoreMaxLength(sseMaxLen >> 1);

        String resolvePrompts = agentPromptsResolver.resolvePrompts(ctx);
        log.debug("当前模式：{}，系统提示词：\n{}", ctx.getIteratorState().getAgentMode().name(), resolvePrompts);
        return builder.build().prompt()
                .system(resolvePrompts)
                .user(ctx.getOriginalRequest().getMessage())
                .toolContext(Map.of("agentChatContext", ctx))
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId())
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
    }
}
