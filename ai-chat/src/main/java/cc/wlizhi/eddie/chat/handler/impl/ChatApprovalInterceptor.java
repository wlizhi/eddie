/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
import cc.wlizhi.eddie.common.handler.AbstractApprovalInterceptor;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Sinks;

/**
 * 助手聊天工具审批拦截器
 * <p>
 * 包装原始 {@link ToolCallback}，继承 {@link AbstractApprovalInterceptor}，
 * 使用 {@link Sinks.Many} 通过 SSE 向前端发射 {@code pending_approval} 事件。
 * <p>
 * 调用方在包装前已按 {@code enabled == 2}（PENDING_APPROVAL）过滤。
 */
public class ChatApprovalInterceptor extends AbstractApprovalInterceptor {

    private final Sinks.Many<ToolExecutionEvent> sink;
    private final Long userMessageId;

    /**
     * @param delegate      原始工具回调
     * @param eventRegistry 事件注册表
     * @param msgId         占位消息 ID（用于审批 key）
     * @param userMessageId 用户消息 ID（用于停止事件 key）
     * @param sink          工具执行事件 Sink（用于发射 pending_approval 事件）
     */
    public ChatApprovalInterceptor(ToolCallback delegate, EventRegistry eventRegistry,
                                   Long msgId, Long userMessageId,
                                   Sinks.Many<ToolExecutionEvent> sink) {
        super(delegate, eventRegistry, "assistant", msgId, null);
        this.userMessageId = userMessageId;
        this.sink = sink;
    }

    @Override
    protected void emitPendingApproval(String toolName, String toolInput) {
        try {
            ToolExecutionEvent event = new ToolExecutionEvent();
            event.setStatus(ToolExecutionStatus.PENDING_APPROVAL);
            event.setToolName(toolName);
            event.setArguments(toolInput);
            event.setSeq(getToolCallSequence());
            var result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.warn("发射 pending_approval 事件失败: toolName={}, reason={}", toolName, result);
            }
        } catch (Exception e) {
            log.warn("发射 pending_approval 事件异常: toolName={}", toolName, e);
        }
    }

    @Override
    protected String getStopEventKey() {
        if (userMessageId == null) return null;
        return EventRegistry.key("STOP", userMessageId.toString());
    }
}
