package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.memory.context.BuiltInPromptsContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AgentPromptsResolver {
    @Resource
    private BuiltInPromptsContext builtInPromptsContext;

    public String resolveChatPrompts(AgentChatContext context) {
        String agentChatPrompts = builtInPromptsContext.getAgentChatPrompts();
        String systemPrompt = context.getAgent().getSystemPrompt();
        return builtInPromptsContext.resolvePrompt(agentChatPrompts, Map.of(
                "msgId", context.getAgentMsg().getId().toString(),
                "userSystemPrompt", systemPrompt
        ));
    }

    public String resolvePrompts(AgentChatContext context) {
        if (context.getAgentMode() == AgentMode.CHAT) {
            return resolveChatPrompts(context);
        }
        return resolveChatPrompts(context);
    }
}
