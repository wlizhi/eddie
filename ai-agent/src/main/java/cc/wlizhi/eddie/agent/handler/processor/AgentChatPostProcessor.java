package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.handler.AgentToolCallbackWrapper;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AgentChatPostProcessor implements AgentClientPostProcessor {
    @Resource
    private ToolCallbackResolver toolCallbackResolver;
    @Resource
    private AgentPromptsResolver agentPromptsResolver;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.CHAT == agentMode;
    }

    @Override
    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext ctx) {
        ToolCallback[] toolCallbacks = toolCallbackResolver.resolve(
                RoleType.AGENT.name(), ctx.getAgent().getId()
                , ctx.getOriginalRequest().getToolSelectionMode()
                , ctx.getOriginalRequest().getToolNames());
        ChatClient client;
        if (toolCallbacks == null || toolCallbacks.length == 0) {
            client = ctx.getChatClient().mutate().build();
        } else {
            Object[] wrappers = Arrays.stream(toolCallbacks)
                    .map(t -> new AgentToolCallbackWrapper(t, ctx)).toArray();
            client = ctx.getChatClient().mutate()
                    .defaultTools(wrappers)
                    .build();
        }
        return client.prompt()
                .system(agentPromptsResolver.resolvePrompts(ctx))
                .user(ctx.getOriginalRequest().getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId())
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
    }
}
