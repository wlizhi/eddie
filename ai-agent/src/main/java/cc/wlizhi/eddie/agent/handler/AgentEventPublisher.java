/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 事件发布器 — 统一管理所有 SSE 事件的构建与发射。
 * <p>
 * 每个事件 payload 统一包装为 JSON envelope：
 * <pre>{@code
 * {"msgId":123, "stepId":null, "data":{...}}
 * }</pre>
 * SSE event name 为 AgentEvent 枚举名的小写下划线形式（如 {@code thinking}、{@code tool_execution}）。
 * <p>
 * 调用方只需关心业务数据，序列化与 envelope 包装由此类统一处理。
 */
@Component
public class AgentEventPublisher {

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 通用发射 ====================

    /**
     * 发射消息级别事件（无 stepId）
     */
    public void emit(AgentChatContext ctx, AgentEvent event, Object data) {
        emit(ctx, event, null, data);
    }

    /**
     * 发射步骤级别事件（含 stepId）
     */
    public void emit(AgentChatContext ctx, AgentEvent event, Long stepId, Object data) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("msgId", msgId);
        envelope.put("stepId", stepId);
        envelope.put("data", data != null ? data : Map.of());

        try {
            String json = objectMapper.writeValueAsString(envelope);
            String sseEventName = event.name().toLowerCase();
            ctx.getSink().next(ServerSentEvent.<String>builder()
                    .event(sseEventName)
                    .data(json)
                    .build());
        } catch (JsonProcessingException e) {
            // 序列化失败不应影响主流程，降级推送错误提示
            ctx.getSink().next(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"msgId\":" + msgId + ",\"stepId\":" + stepId + ",\"data\":{\"message\":\"Event serialization error\"}}")
                    .build());
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 消息已创建
     * <p>
     * 发送 {@code userMsgId} 和 {@code assistantMsgId} 供前端缓存，
     * 前端停止请求时使用 {@code assistantMsgId} 作为 {@code messageId}。
     */
    public void messageCreated(AgentChatContext ctx) {
        Long userMsgId = ctx.getUserMsg() != null ? ctx.getUserMsg().getId() : null;
        Long assistantMsgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        emit(ctx, AgentEvent.MESSAGE_CREATED, Map.of(
                "userMsgId", userMsgId,
                "assistantMsgId", assistantMsgId
        ));
    }

    /**
     * 模型思考内容（流式）
     */
    public void thinking(AgentChatContext ctx, Long stepId, String text) {
        emit(ctx, AgentEvent.THINKING, stepId, Map.of("text", text));
    }

    /**
     * 模型回答内容（流式）
     */
    public void answer(AgentChatContext ctx, Long stepId, String text) {
        emit(ctx, AgentEvent.ANSWER, stepId, Map.of("text", text));
    }

    /**
     * 工具执行结果
     */
    public void toolExecution(AgentChatContext ctx, Long stepId, String toolName, String status, Object result) {
        emit(ctx, AgentEvent.TOOL_EXECUTION, stepId, Map.of(
                "toolName", toolName,
                "status", status,
                "result", result
        ));
    }

    /**
     * 更新任务清单（全量推送）
     */
    public void updateTaskPlan(AgentChatContext ctx, Object taskPlan) {
        emit(ctx, AgentEvent.UPDATE_TASK_PLAN, taskPlan);
    }

    /**
     * 循环开始
     */
    public void roundStart(AgentChatContext ctx, int round) {
        emit(ctx, AgentEvent.ROUND_START, Map.of("round", round));
    }

    /**
     * 元数据（token 用量、耗时等）
     */
    public void metadata(AgentChatContext ctx, AgentTokenStatists stats) {
        emit(ctx, AgentEvent.METADATA, stats);
    }

    /**
     * 任务取消
     */
    public void cancelled(AgentChatContext ctx, String reason) {
        emit(ctx, AgentEvent.CANCELLED, Map.of("reason", reason));
    }

    /**
     * 执行过程错误
     */
    public void error(AgentChatContext ctx, String message) {
        emit(ctx, AgentEvent.ERROR, Map.of("message", message));
    }

    /**
     * 任务结束
     */
    public void taskFinish(AgentChatContext ctx) {
        emit(ctx, AgentEvent.TASK_FINISH, Map.of());
    }

    // ==================== 内部模式切换事件 ====================

    /**
     * 发射内部模式切换事件（不走 SSE，走 EventRegistry）
     */
    public void switchMode(AgentChatContext ctx, AgentEvent modeEvent) {
        // 模式切换事件通过 EventRegistry 在虚拟线程间通信
        // 暂不在此处实现，由 switchModeIfNecessary 处理
    }
}
