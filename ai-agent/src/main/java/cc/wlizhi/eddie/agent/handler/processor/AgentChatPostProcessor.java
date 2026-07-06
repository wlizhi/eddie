package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.handler.AgentToolCallbackWrapper;
import cc.wlizhi.eddie.agent.service.impl.AgentShortTermMemory;
import cc.wlizhi.eddie.agent.tool.AgentToolProvider;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class AgentChatPostProcessor implements AgentClientPostProcessor {
    @Resource
    private ToolCallbackResolver toolCallbackResolver;
    @Resource
    private AgentPromptsResolver agentPromptsResolver;
    @Resource
    private List<AgentToolProvider> agentToolProviders;
    @Resource
    private AgentShortTermMemory agentShortTermMemory;

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

        // 2. 收集所有工具（用户可配 + 智能体内置）
        List<ToolCallback> allTools = new ArrayList<>();
        if (configurableTools != null) {
            allTools.addAll(Arrays.asList(configurableTools));
        }
        for (AgentToolProvider provider : agentToolProviders) {
            ToolCallback[] internalTools = ToolCallbacks.from(provider);
            allTools.addAll(Arrays.asList(internalTools));
        }

        // 3. 构建 ChatClient（仅在 chatting 模式注入记忆窗口 advisor）
        ChatClient.Builder builder = ctx.getChatClient().mutate();
        if (ctx.getIteratorState().getAgentMode() == AgentMode.CHAT) {
            var memoryAdvisor = MessageChatMemoryAdvisor.builder(agentShortTermMemory).build();
            builder.defaultAdvisors(memoryAdvisor);
        }
        if (!allTools.isEmpty()) {
            Object[] wrappers = allTools.stream()
                    .map(t -> new AgentToolCallbackWrapper(t, ctx)).toArray();
            builder.defaultTools(wrappers);
        }

        return builder.build().prompt()
                .system(agentPromptsResolver.resolvePrompts(ctx))
                .user(ctx.getOriginalRequest().getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId())
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
    }
}
