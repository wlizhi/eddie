/**
 * ToolCallbackWrapper — 工具回调包装器
 * <p>
 * 在 Spring AI 的 ToolCallback 执行前后插入事件发射逻辑，将工具执行状态通过 Sinks
 * 旁路发射到 SSE 流中，使前端能实时看到"模型正在调用工具"的中间状态。
 * <p>
 * 包装器不改变原始 ToolCallback 的调用语义，工具仍只会被执行一次。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.handler.AbstractApprovalInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Sinks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ToolCallbackWrapper implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ToolCallbackWrapper.class);

    private final ToolCallback delegate;
    private final Sinks.Many<ToolExecutionEvent> sink;
    private final AtomicInteger toolCallCounter;
    private final int MAX_RESULT_LENGTH = 3000;

    public ToolCallbackWrapper(ToolCallback delegate, Sinks.Many<ToolExecutionEvent> sink,
                               AtomicInteger toolCallCounter) {
        this.delegate = delegate;
        this.sink = sink;
        this.toolCallCounter = toolCallCounter;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        String toolName = delegate.getToolDefinition().name();

        // 在调用前设置唯一工具调用序号（用于审批 key 去重）
        setSequenceOnInterceptor();
        int currentSeq = resolveSequence();

        // 发射"开始"事件（旁路，不影响主流程）
        ToolExecutionEvent startEvent = ToolExecutionEvent.start(toolName, toolInput);
        startEvent.setSeq(currentSeq);
        emitSafe(startEvent);

        try {
            // 执行实际工具——仅此一次
            String result = delegate.call(toolInput);

            // 检查是否被用户拒绝
            boolean rejected = delegate instanceof AbstractApprovalInterceptor ai && ai.isRejected();
            ToolExecutionEvent event = rejected
                    ? ToolExecutionEvent.rejected(toolName, toolInput)
                    : ToolExecutionEvent.complete(toolName, toolInput, truncateResult(result), false);
            event.setSeq(resolveSequence());
            emitSafe(event);

            return result;
        } catch (Exception e) {
            log.error("[ToolCallbackWrapper] 工具执行失败: {}", toolName, e);
            ToolExecutionEvent event = ToolExecutionEvent.complete(toolName, toolInput, "错误: " + e.getMessage(), true);
            event.setSeq(resolveSequence());
            emitSafe(event);
            throw e;
        }
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = delegate.getToolDefinition().name();

        // 在调用前设置唯一工具调用序号（用于审批 key 去重）
        setSequenceOnInterceptor();
        int currentSeq = resolveSequence();

        ToolExecutionEvent startEvent = ToolExecutionEvent.start(toolName, toolInput);
        startEvent.setSeq(currentSeq);
        emitSafe(startEvent);

        try {
            String result = delegate.call(toolInput, toolContext);
            boolean rejected = delegate instanceof AbstractApprovalInterceptor ai && ai.isRejected();
            ToolExecutionEvent event = rejected
                    ? ToolExecutionEvent.rejected(toolName, toolInput)
                    : ToolExecutionEvent.complete(toolName, toolInput, truncateResult(result), false);
            event.setSeq(resolveSequence());
            emitSafe(event);
            return result;
        } catch (Exception e) {
            log.error("[ToolCallbackWrapper] 工具执行失败: {}", toolName, e);
            ToolExecutionEvent event = ToolExecutionEvent.complete(toolName, toolInput, "错误: " + e.getMessage(), true);
            event.setSeq(resolveSequence());
            emitSafe(event);
            throw e;
        }
    }

    /**
     * 在调用 delegate.call() 前，将当前序号设置到拦截器上。
     * 拦截器的 call() 方法会使用此序号构建唯一审批 key。
     */
    private void setSequenceOnInterceptor() {
        if (delegate instanceof AbstractApprovalInterceptor ai) {
            int seq = toolCallCounter.incrementAndGet();
            ai.setToolCallSequence(seq);
        }
    }

    /**
     * 解析当前工具调用序号（用于 SSE 负载）
     */
    private int resolveSequence() {
        if (delegate instanceof AbstractApprovalInterceptor ai) {
            return ai.getToolCallSequence();
        }
        return 0;
    }

    /**
     * 安全发射事件，失败仅打日志，不影响主流程
     */
    private void emitSafe(ToolExecutionEvent event) {
        try {
            var result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.debug("[ToolCallbackWrapper] 发射工具事件失败: {} (reason={})",
                        event.getToolName(), result);
            }
        } catch (Exception e) {
            log.debug("[ToolCallbackWrapper] 发射工具事件异常", e);
        }
    }

    /**
     * 截断过长结果（避免SSE数据过大），同时修复转义换行符
     * <p>
     * 某些外部工具返回的内容中可能包含字面量 {@code \n}，而非真实换行符，
     * 导致前端 Markdown 渲染异常。此处统一替换为真实换行符。
     */
    private String truncateResult(String result) {
        return result;
//        if (result == null) return "";
//        // 修复字面量 \n → 真实换行符
////        String fixed = result.replace("\\n", "\n");
//        if (result.length() > MAX_RESULT_LENGTH) {
//            return result.substring(0, MAX_RESULT_LENGTH) + "...（已截断）";
//        }
//        return result;
    }
}
