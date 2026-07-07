package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
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
        return builtInPromptsContext.resolvePrompt(planPrompts, Map.of());
    }

    private String resolveExecutePrompts(AgentChatContext context) {
        String executePrompts = builtInPromptsContext.getAgentTaskExecutePrompts();
        if (executePrompts == null) {
            log.warn("Agent task execute prompts is null, fallback to chat prompts");
            return resolveChatPrompts(context);
        }

        // 当前步骤编号
        Integer currentStep = context.getCurrentStep();

        // 完整 taskPlan 序列化为 JSON
        AgentTaskPlan taskPlan = context.getTaskPlan();
        String taskPlanJson;
        try {
            taskPlanJson = context.getObjectMapper().writeValueAsString(taskPlan);
        } catch (Exception e) {
            log.warn("Failed to serialize taskPlan to JSON, use empty", e);
            taskPlanJson = "{}";
        }

        return builtInPromptsContext.resolvePrompt(executePrompts, Map.of(
                "stepNumber", String.valueOf(currentStep),
                "taskPlan", taskPlanJson,
                "msgId", context.getAgentMsg().getId().toString()
        ));
    }

    public String resolvePrompts(AgentChatContext context) {
        if (context.getIteratorState().getAgentMode() == AgentMode.CHAT) {
            return resolveChatPrompts(context);
        }
        if (context.getIteratorState().getAgentMode() == AgentMode.PLAN) {
            return resolvePlanPrompts(context);
        }
        if (context.getIteratorState().getAgentMode() == AgentMode.EXECUTE) {
            return resolveExecutePrompts(context);
        }
        return resolveChatPrompts(context);
    }
}
