/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.dao.AgentMsgStepDao;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.dto.*;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.agent.enums.TaskPlanStatus;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * EXECUTE 模式流式响应处理器
 * <p>
 * 执行模式：负责处理任务计划中单个步骤的模型交互流。
 * <p>
 * 核心职责：
 * <ol>
 *     <li>流开始前获取 {@link AgentStepStreamContext} 步骤级累加器（由 {@link ExecuteClientPostProcessor} 创建并预置 stepRecordId）；
 *         若该步骤序号首次执行则推送 {@code step_started} 事件</li>
 *     <li>流处理中覆写 {@code handleThinking/handleAnswer}，使用步骤级累加器独立存储，
 *         用真实 stepRecordId 发射事件（父类保持 {@code null}，不影响消息级别）</li>
 *     <li>流结束后将占位记录更新为步骤累加器的实际内容，推送 {@code execute_complete} 事件</li>
 * </ol>
 * <p>
 * 每次模型交互（可能包含多轮 tool_call 自循环）产生一条步骤记录。
 * Token 统计等元数据由父类 {@link AbstractStreamProcessor#afterStream} 在消息级别统一处理。
 */
@Component
public class ExecuteResponseStreamProcessor extends AbstractStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(ExecuteResponseStreamProcessor.class);

    @Resource
    private AgentMsgStepDao agentMsgStepDao;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.EXECUTE == agentMode;
    }


    @Override
    protected void handleThinking(AgentChatContext ctx, ChatResponse response) {
        try {
            var metadata = Objects.requireNonNull(response.getResult()).getOutput().getMetadata();
            Object reasoning = metadata.get("reasoningContent");
            if (reasoning == null) {
                reasoning = metadata.get("reasoning_content");
            }
            if (reasoning != null && !reasoning.toString().isEmpty()) {
                String text = reasoning.toString();
                AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
                if (stepCtx != null) {
                    // 用真实 stepRecordId 发射（父类发射的 stepRecordId=null，不影响消息级别）
                    publisher.thinking(ctx, stepCtx.getStepRecordId(), text);
                    // 累加到步骤级（独立于消息级 fullThinking）
                    int thinkLength = stepCtx.getFullThinking().length();
                    if (thinkLength < 500) {
                        stepCtx.getFullThinking().append(text);
                    } else if (stepCtx.getFullThinking().charAt(stepCtx.getFullThinking().length() - 1) != '.') {
                        stepCtx.getFullThinking().append("...");
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    @Override
    protected void handleAnswer(AgentChatContext ctx, ChatResponse response) {
        try {
            String answer = response.getResults().stream()
                    .map(org.springframework.ai.chat.model.Generation::getOutput)
                    .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                    .filter(Objects::nonNull)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.joining());
            if (!answer.isEmpty()) {
                AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
                if (stepCtx != null) {
                    // 用真实 stepRecordId 发射（父类发射的 stepRecordId=null，不影响消息级别）
                    publisher.answer(ctx, stepCtx.getStepRecordId(), answer);
                    // 累加到步骤级（独立于消息级 fullAnswer）
                    stepCtx.getFullAnswer().append(answer);
                }
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    @Override
    protected void afterStream(AgentChatContext ctx, ChatResponse lastResponse) {
        // 1. 基类通用逻辑已在 AbstractStreamProcessor.process() 中完成（token 提取 + 推送 + 持久化）

        // 2. 更新占位记录的实际内容（步骤级别，使用独立累加器）
        updateStepRecord(ctx);

        // 3. 更新迭代状态到消息表
        AgentIteratorState iteratorState = ctx.getIteratorState();
        if (iteratorState != null && ctx.getAgentMsg() != null && ctx.getAgentMsg().getId() != null) {
            try {
                String iteratorStateJson = ctx.getEvent().getObjectMapper().writeValueAsString(iteratorState);
                agentMsgDao.updateIteratorState(ctx.getAgentMsg().getId(), iteratorStateJson);
                log.info("迭代状态更新完成, msgId={}, iteratorState={}", ctx.getAgentMsg().getId(), iteratorStateJson);
            } catch (Exception e) {
                log.warn("迭代状态序列化或持久化失败, msgId={}: {}", ctx.getAgentMsg().getId(), e.getMessage());
            }
        }

        // 4. 检测当前步骤是否完成，推进步骤状态机
        handleStepOnFinish(ctx);
    }

    @Override
    protected void onStreamInterrupted(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null) {
            return;
        }

        // 同步更新 stepCtx 和 taskPlan 中当前步骤的状态为 INTERRUPTED
        stepCtx.setStepStatus(StepStatus.INTERRUPTED);

        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan != null && taskPlan.getSteps() != null) {
            int currentStepNumber = stepCtx.getStepNumber() != null
                    ? stepCtx.getStepNumber()
                    : ctx.getMetrics().getCurrentStepNumber();
            if (currentStepNumber > 0 && currentStepNumber <= taskPlan.getSteps().size()) {
                AgentTaskStep currentPlanStep = taskPlan.getSteps().get(currentStepNumber - 1);
                currentPlanStep.setStatus(StepStatus.INTERRUPTED.getValue());
            }
        }

        // 持久化 taskPlan 到 DB
        persistTaskPlan(ctx);
        log.info("流处理被中断，步骤 {} 已标记为 INTERRUPTED", stepCtx.getStepNumber());
    }

    /**
     * 流结束后将占位记录更新为实际累积的内容（thinking + content + toolCalls）。
     * 数据来源为步骤级累加器 {@link AgentStepStreamContext}，独立于消息级别累加器。
     */
    private void updateStepRecord(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null || stepCtx.getStepRecordId() == null) {
            log.warn("stepStreamContext 或 stepRecordId 为空，跳过步骤记录更新");
            return;
        }

        String content = stepCtx.getFullAnswer().toString();
        String thinking = stepCtx.getFullThinking().toString();

        // 序列化工具调用记录
        String toolCallsJson = "[]";
        List<ChatToolExecutionEvent> toolCalls = stepCtx.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            try {
                toolCallsJson = ctx.getEvent().getObjectMapper().writeValueAsString(toolCalls);
            } catch (Exception e) {
                log.warn("序列化工具调用记录失败, stepRecordId={}: {}", stepCtx.getStepRecordId(), e.getMessage());
            }
        }

        try {
            agentMsgStepDao.updateContent(stepCtx.getStepRecordId(), content, thinking, toolCallsJson);
            log.info("步骤记录更新完成, stepRecordId={}, contentLen={}, thinkingLen={}, toolCallsSize={}",
                    stepCtx.getStepRecordId(), content.length(), thinking.length(),
                    toolCalls != null ? toolCalls.size() : 0);
        } catch (Exception e) {
            log.warn("步骤记录更新失败, stepRecordId={}: {}", stepCtx.getStepRecordId(), e.getMessage());
        }
    }

    /**
     * 构建占位步骤实体（content/thinking/toolCalls 为空，由 {@link #afterStream} 填充）
     */
    static AgentMsgStepEntity buildPlaceholderEntity(AgentChatContext ctx,
                                                             Integer currentStep, String stepDesc) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        // 优先从步骤累加器获取 prompt（由 AgentExecutePostProcessor 初始化时写入）
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        String prompt = stepCtx != null && stepCtx.getPrompt() != null
                ? stepCtx.getPrompt()
                : "请根据系统提示词及历史消息（如果有）继续完成当前步骤的任务内容";

        AgentMsgStepEntity entity = new AgentMsgStepEntity();
        entity.setMsgId(msgId);
        entity.setMsgType(0);
        entity.setMsgDataType(0);
        entity.setStepNumber(currentStep != null ? currentStep : 0);
        entity.setStepDesc(stepDesc);
        entity.setPrompt(prompt);
        entity.setCreatedAt(System.currentTimeMillis());
        return entity;
    }

    /**
     * 从任务计划中解析当前步骤的描述信息（标题）
     */
    static String resolveStepDesc(AgentChatContext ctx, Integer currentStepNumber) {
        if (currentStepNumber == null || currentStepNumber <= 0) {
            return "";
        }
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null || taskPlan.getSteps() == null) {
            return "";
        }
        List<AgentTaskStep> steps = taskPlan.getSteps();
        if (currentStepNumber > steps.size()) {
            return "";
        }
        AgentTaskStep step = steps.get(currentStepNumber - 1);
        return step.getTitle() != null ? step.getTitle() : "";
    }

    /**
     * 检测当前步骤是否已完成或失败，若是则推进到下一步或结束任务。
     * <p>
     * 步骤失败（FAILED）时采用"继续执行"策略：任务标记 FAILED 但仍尝试执行剩余步骤。
     */
    private void handleStepOnFinish(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null) {
            return;
        }
        StepStatus status = stepCtx.getStepStatus();
        if (status != StepStatus.COMPLETED && status != StepStatus.FAILED) {
            return;
        }
        if (ctx.getIteratorState().getAgentMode() != AgentMode.EXECUTE) {
            return;
        }

        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null || taskPlan.getSteps() == null || taskPlan.getSteps().isEmpty()) {
            return;
        }

        boolean isFailed = status == StepStatus.FAILED;
        // 使用 stepStreamContext.stepNumber（工具实际更新的步骤编号）而非 ctx.getCurrentStepNumber()
        int currentStepNumber = stepCtx.getStepNumber() != null ? stepCtx.getStepNumber() : ctx.getMetrics().getCurrentStepNumber();
        int totalSteps = taskPlan.getSteps().size();

        if (currentStepNumber < totalSteps) {
            // 推进到下一步
            int nextStepNumber = currentStepNumber + 1;
            ctx.getMetrics().setCurrentStepNumber(nextStepNumber);
            AgentTaskStep nextPlanStep = taskPlan.getSteps().get(nextStepNumber - 1);
            nextPlanStep.setStatus(StepStatus.PROCESSING.getValue());
            log.info("步骤 {} {}，推进至步骤 {}", currentStepNumber, isFailed ? "失败" : "完成", nextStepNumber);
        } else {
            // 最后一步完成或失败
            taskPlan.setStatus(isFailed ? TaskPlanStatus.FAILED.getValue() : TaskPlanStatus.COMPLETED.getValue());
            taskPlan.setResult(taskPlan.getSteps().getLast().getResult());
            ctx.getIteratorState().setFinished(true);
            log.info("所有步骤执行{}，设置 finished=true，迭代循环即将退出", isFailed ? "（含失败步骤）" : "完成");
        }

        // 持久化 taskPlan 并推送更新事件
        persistTaskPlan(ctx);
        ctx.getEvent().getEventPublisher().updateTaskPlan(ctx, taskPlan);
    }

    /**
     * 持久化 taskPlan 到数据库
     */
    private void persistTaskPlan(AgentChatContext ctx) {
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null) {
            return;
        }
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (msgId == null) {
            return;
        }
        try {
            String taskPlanJson = ctx.getEvent().getObjectMapper().writeValueAsString(taskPlan);
            agentMsgDao.updateTaskPlan(msgId, taskPlanJson);
        } catch (Exception e) {
            log.warn("持久化 taskPlan 失败, msgId={}: {}", msgId, e.getMessage());
        }
    }
}
