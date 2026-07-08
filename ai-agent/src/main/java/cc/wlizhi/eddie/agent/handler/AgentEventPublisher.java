/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentStepStreamContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import cc.wlizhi.eddie.agent.entity.event.payload.*;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

/**
 * Agent 事件发布器 — 统一管理所有 SSE 事件的构建与发射。
 * <p>
 * 所有事件统一使用 {@link ApiResult} 作为数据容器，<code>data</code> 字段为独立的 Payload 实体类。
 * SSE event name 为 {@link AgentEvent} 枚举名的小写下划线形式（如 {@code thinking}、{@code tool_execution}）。
 * <p>
 * 前端统一通过 {@code code === 200} 判断事件是否成功，与 REST API 同一套语义。
 */
@Component
public class AgentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 通用发射 ====================

    /**
     * 发射 SSE 事件。从 ctx 自动提取 msgId/stepId/step。
     *
     * @param ctx     上下文
     * @param event   事件类型
     * @param result  已构建的 ApiResult（包含 code/message/detail/data）
     */
    public <T> void emit(AgentChatContext ctx, AgentEvent event, ApiResult<T> result) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        try {
            String json = objectMapper.writeValueAsString(result);
            String sseEventName = event.name().toLowerCase();
            ctx.getSink().next(ServerSentEvent.<String>builder()
                    .event(sseEventName)
                    .data(json)
                    .build());
        } catch (JsonProcessingException e) {
            log.warn("SSE 事件序列化失败, event={}, msgId={}", event, msgId, e);
            emitFallbackError(ctx, msgId, "SSE 事件序列化失败: " + event.name());
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 消息已创建
     */
    public void messageCreated(AgentChatContext ctx) {
        Long userMsgId = ctx.getUserMsg() != null ? ctx.getUserMsg().getId() : null;
        Long assistantMsgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        MessageCreatedPayload payload = new MessageCreatedPayload(
                msgId, null, null, userMsgId, assistantMsgId);
        emit(ctx, AgentEvent.MESSAGE_CREATED, ApiResult.success(payload));
    }

    /**
     * 模型思考内容（流式）
     */
    public void thinking(AgentChatContext ctx, Long stepId, String text) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        Integer step = resolveStep(ctx);
        ThinkingPayload payload = new ThinkingPayload(msgId, stepId, step, text);
        emit(ctx, AgentEvent.THINKING, ApiResult.success(payload));
    }

    /**
     * 模型回答内容（流式）
     */
    public void answer(AgentChatContext ctx, Long stepId, String text) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        Integer step = resolveStep(ctx);
        AnswerPayload payload = new AnswerPayload(msgId, stepId, step, text);
        emit(ctx, AgentEvent.ANSWER, ApiResult.success(payload));
    }

    /**
     * 工具执行结果
     */
    public void toolExecution(AgentChatContext ctx, Long stepId, String toolName, String status, Object result) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        Integer step = resolveStep(ctx);
        ToolExecutionPayload payload = new ToolExecutionPayload(
                msgId, stepId, step, toolName, status, null, result, false);
        emit(ctx, AgentEvent.TOOL_EXECUTION, ApiResult.success(payload));
    }

    /**
     * 规划开始
     */
    public void planStarted(AgentChatContext ctx) {
        emit(ctx, AgentEvent.PLAN_STARTED, ApiResult.success());
    }

    /**
     * 规划生成成功
     */
    public void planGenerated(AgentChatContext ctx, AgentTaskPlan taskPlan) {
        emit(ctx, AgentEvent.PLAN_GENERATED, ApiResult.success(taskPlan));
    }

    /**
     * 更新任务清单（全量推送）
     */
    public void updateTaskPlan(AgentChatContext ctx, AgentTaskPlan taskPlan) {
        emit(ctx, AgentEvent.UPDATE_TASK_PLAN, ApiResult.success(taskPlan));
    }

    /**
     * 循环开始
     */
    public void roundStart(AgentChatContext ctx, int round) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        RoundStartPayload payload = new RoundStartPayload(msgId, null, null, round);
        emit(ctx, AgentEvent.ROUND_START, ApiResult.success(payload));
    }

    /**
     * 元数据（token 用量、耗时等）
     */
    public void metadata(AgentChatContext ctx, AgentTokenStatists stats) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        Integer step = resolveStep(ctx);
        MetadataPayload payload = new MetadataPayload(msgId, null, step, stats);
        emit(ctx, AgentEvent.METADATA, ApiResult.success(payload));
    }

    /**
     * 任务取消
     */
    public void cancelled(AgentChatContext ctx, String reason) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        CancelledPayload payload = new CancelledPayload(msgId, null, null, reason);
        emit(ctx, AgentEvent.CANCELLED, ApiResult.success(payload));
    }

    /**
     * 执行过程错误（结构化错误信息）
     */
    public void error(AgentChatContext ctx, ApiResultCode resultCode, String message, String detail) {
        emit(ctx, AgentEvent.ERROR, ApiResult.error(resultCode, message, detail));
    }

    /**
     * 任务结束
     */
    public void taskFinish(AgentChatContext ctx) {
        emit(ctx, AgentEvent.TASK_FINISH, ApiResult.success());
    }

    // ==================== 内部方法 ====================

    /**
     * 从 StepStreamContext 解析当前步骤序号
     */
    private Integer resolveStep(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx != null && stepCtx.getStep() != null) {
            return stepCtx.getStep();
        }
        return ctx.getCurrentStep();
    }

    /**
     * 降级：序列化失败时发送纯字符串备用
     */
    private void emitFallbackError(AgentChatContext ctx, Long msgId, String text) {
        String fallback = "{\"code\":1000,\"message\":\"" + text + "\",\"detail\":null,\"data\":null}";
        ctx.getSink().next(ServerSentEvent.<String>builder()
                .event("error")
                .data(fallback)
                .build());
    }
}
