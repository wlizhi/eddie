package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.memory.context.BuiltInPromptsContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
//                "completedResult", buildCompletedResult(context)
        ));
    }

    /**
     * 构建 ${completedResult} 变量的值：当前步骤所依赖步骤的执行结果。<p>
     * 每个步骤以 Markdown 三级标题 {@code ### 步骤 N 执行结果} 开头，后跟步骤内容。
     * 示例：
     * <pre>
     * ### 步骤 1 执行结果
     * xxx
     * ### 步骤 3 执行结果
     * xxx
     * </pre>
     * 数据来源为内存中的 {@link AgentChatContext#getTaskStepList()}，该列表由
     * {@link cc.wlizhi.eddie.agent.handler.processor.ExecuteResponseStreamProcessor#updateStepRecord}
     * 在每轮流结束后先更新内存实体再持久化到 DB。
     */
    private String buildCompletedResult(AgentChatContext context) {
        Integer currentStep = context.getCurrentStep();
        AgentTaskPlan taskPlan = context.getTaskPlan();
        if (currentStep == null || currentStep <= 0 || taskPlan == null
                || taskPlan.getSteps() == null || currentStep > taskPlan.getSteps().size()) {
            return "";
        }

        // 获取当前步骤的依赖步骤 ID 列表
        AgentTaskStep stepDef = taskPlan.getSteps().get(currentStep - 1);
        List<Integer> dependsOn = stepDef.getDependsOn();
        if (dependsOn == null || dependsOn.isEmpty()) {
            return "";
        }

        List<List<AgentMsgStepEntity>> stepList = context.getTaskStepList();
        if (stepList == null) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (Integer depStepId : dependsOn) {
            if (depStepId == null || depStepId <= 0 || depStepId - 1 >= stepList.size()) {
                continue;
            }
            List<AgentMsgStepEntity> entities = stepList.get(depStepId - 1);
            String content;
            if (entities == null || entities.isEmpty()) {
                content = "（暂无执行结果）";
            } else {
                // 取最后一条实体的 content（最新一轮的执行结果）
                AgentMsgStepEntity lastEntity = entities.get(entities.size() - 1);
                String lastContent = lastEntity.getContent();
                if (lastContent == null || lastContent.isBlank()) {
                    content = "（暂无执行结果）";
                } else {
                    content = lastContent;
                }
            }
            lines.add("### 步骤 " + depStepId + " 执行结果\n" + content);
        }
        return String.join("\n", lines);
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
