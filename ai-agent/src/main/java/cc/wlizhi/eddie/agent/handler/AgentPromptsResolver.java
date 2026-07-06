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

    public String resolvePlanPrompts(AgentChatContext context) {
        String planPrompts = builtInPromptsContext.getAgentTaskPlanPrompts();
        // TODO 根本不需要自定义参数，直接在提示词模板中写参数，解析类自动识别
        return builtInPromptsContext.resolvePrompt(planPrompts, Map.of(
                "datetime", context.getOriginalRequest().getMessage(),
                "timezone", "Asia/Shanghai",
                "os", System.getProperty("os.name", "Unknown"),
                "language", "zh-CN"
        ));
    }

    public String resolvePrompts(AgentChatContext context) {
        if (context.getIteratorState().getAgentMode() == AgentMode.CHAT) {
            return resolveChatPrompts(context);
        }
        if (context.getIteratorState().getAgentMode() == AgentMode.PLAN) {
            return resolvePlanPrompts(context);
        }
        return resolveChatPrompts(context);
    }
}
