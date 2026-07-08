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
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
 *     <li>流开始前创建 {@link AgentStepStreamContext} 步骤级累加器，预创建占位记录获取 stepId；
 *         若该步骤序号首次执行则推送 {@code step_started} 事件</li>
 *     <li>流处理中覆写 {@code handleThinking/handleAnswer}，使用步骤级累加器独立存储，
 *         用真实 stepId 发射事件（父类保持 {@code null}，不影响消息级别）</li>
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
    protected void beforeStream(AgentChatContext ctx) {
        Integer currentStep = ctx.getCurrentStep();
        String stepDesc = resolveStepDesc(ctx, currentStep);

        // 1. 获取已初始化的步骤级流式累加器（由 AgentExecutePostProcessor 创建并设好 prompt）
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null) {
            // 容错：如果前置未初始化，兜底创建
            stepCtx = new AgentStepStreamContext();
            ctx.setStepStreamContext(stepCtx);
        }

        stepCtx.setStep(currentStep);
        stepCtx.setStepDesc(stepDesc);

        // 3. 预创建占位记录，拿到 stepId
        AgentMsgStepEntity placeholder = buildPlaceholderEntity(ctx, currentStep, stepDesc);
        try {
            Long stepId = agentMsgStepDao.insertPlaceholder(placeholder);
            stepCtx.setStepId(stepId);
        } catch (Exception e) {
            log.warn("预创建步骤占位记录失败, msgId={}, step={}: {}",
                    ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null,
                    currentStep, e.getMessage());
        }
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
                    // 用真实 stepId 发射（父类发射的 stepId=null，不影响消息级别）
                    publisher.thinking(ctx, stepCtx.getStepId(), text);
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
                    // 用真实 stepId 发射（父类发射的 stepId=null，不影响消息级别）
                    publisher.answer(ctx, stepCtx.getStepId(), answer);
                    // 累加到步骤级（独立于消息级 fullAnswer）
                    stepCtx.getFullAnswer().append(answer);
                }
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    @Override
    protected void afterStream(AgentChatContext ctx) {
        // 1. 基类通用逻辑：token 提取 + 增量持久化 + metadata 推送（消息级别）
        super.afterStream(ctx);

        // 2. 更新占位记录的实际内容（步骤级别，使用独立累加器）
        updateStepRecord(ctx);

        // TODO BUG: 不是一个正常的payload，应当包含消息id，步骤编号，步骤id，当前迭代次数
        // 3. 推送执行完成事件
        ctx.getEventPublisher().emit(ctx, AgentEvent.EXECUTE_COMPLETE,
                ApiResult.success(new HashMap<>()));
    }

    /**
     * 流结束后将占位记录更新为实际累积的内容（thinking + content + toolCalls）。
     * 数据来源为步骤级累加器 {@link AgentStepStreamContext}，独立于消息级别累加器。
     */
    private void updateStepRecord(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null || stepCtx.getStepId() == null) {
            log.warn("stepStreamContext 或 stepId 为空，跳过步骤记录更新");
            return;
        }

        String content = stepCtx.getFullAnswer().toString();
        String thinking = stepCtx.getFullThinking().toString();

        // 序列化工具调用记录
        String toolCallsJson = "[]";
        List<ToolExecutionEvent> toolCalls = stepCtx.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            try {
                toolCallsJson = ctx.getObjectMapper().writeValueAsString(toolCalls);
            } catch (Exception e) {
                log.warn("序列化工具调用记录失败, stepId={}: {}", stepCtx.getStepId(), e.getMessage());
            }
        }

        try {
            agentMsgStepDao.updateContent(stepCtx.getStepId(), content, thinking, toolCallsJson);
            log.info("步骤记录更新完成, stepId={}, contentLen={}, thinkingLen={}, toolCallsSize={}",
                    stepCtx.getStepId(), content.length(), thinking.length(),
                    toolCalls != null ? toolCalls.size() : 0);
        } catch (Exception e) {
            log.warn("步骤记录更新失败, stepId={}: {}", stepCtx.getStepId(), e.getMessage());
        }
    }

    /**
     * 构建占位步骤实体（content/thinking/toolCalls 为空，由 {@link #afterStream} 填充）
     */
    private static AgentMsgStepEntity buildPlaceholderEntity(AgentChatContext ctx,
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
        entity.setStep(currentStep != null ? currentStep : 0);
        entity.setStepDesc(stepDesc);
        entity.setPrompt(prompt);
        entity.setCreatedAt(System.currentTimeMillis());
        return entity;
    }

    /**
     * 从任务计划中解析当前步骤的描述信息（标题）
     */
    private static String resolveStepDesc(AgentChatContext ctx, Integer currentStep) {
        if (currentStep == null || currentStep <= 0) {
            return "";
        }
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null || taskPlan.getSteps() == null) {
            return "";
        }
        List<AgentTaskStep> steps = taskPlan.getSteps();
        if (currentStep > steps.size()) {
            return "";
        }
        AgentTaskStep step = steps.get(currentStep - 1);
        return step.getTitle() != null ? step.getTitle() : "";
    }

    @Override
    protected boolean breakInStreamIfNecessary(AgentChatContext ctx) {
        AgentMode agentMode = ctx.getIteratorState().getAgentMode();
        Integer currentStep = ctx.getCurrentStep();
        List<AgentTaskStep> steps = ctx.getTaskPlan().getSteps();
        String stepStatus = steps.get(currentStep - 1).getStatus();
        boolean isFinal = Objects.equals(stepStatus, StepStatus.COMPLETED.getValue())
                || Objects.equals(stepStatus, StepStatus.FAILED.getValue());
        if (AgentMode.EXECUTE == agentMode && isFinal) {
            return true;
        }
        return super.breakInStreamIfNecessary(ctx);
    }
}
