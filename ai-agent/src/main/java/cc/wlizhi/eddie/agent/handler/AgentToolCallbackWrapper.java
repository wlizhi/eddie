/**
 * AgentToolCallbackWrapper — 智能体工具回调包装器
 * <p>
 * 在智能体循环中，将 ToolCallback 的执行状态通过 SSE 推送到前端。
 * 与 ai-chat 模块的 ToolCallbackWrapper 不同，此处直接使用 FluxSink
 * 推 ServerSentEvent，避免引入 Sinks.Many 的中间层。
 * <p>
 * 可在 call() 中修改工具执行结果再返回给模型（工具结果加工点）。
 * <p>
 * 注意：Spring AI 2.0.0 的 {@link ToolContext#getContext()} 返回不可修改的 Map，
 * 因此不能通过 toolContext.getContext().put() 注入 AgentChatContext。
 * 如需向 @Tool 方法传递 AgentChatContext，应在构建 ChatClient 时通过
 * {@code prompt().toolContext("agentChatContext", ctx)} 设置。
 *
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

@Slf4j
public class AgentToolCallbackWrapper implements ToolCallback {

    private final ToolCallback delegate;
    private final AgentChatContext ctx;

    public AgentToolCallbackWrapper(ToolCallback delegate, AgentChatContext ctx) {
        this.delegate = delegate;
        this.ctx = ctx;
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
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = delegate.getToolDefinition().name();

        // Spring AI 2.0.0: ToolContext.getContext() 返回不可修改 Map，不可调用 put()
        // AgentChatContext 已通过 ChatClientRequestSpec.toolContext() 注入到 ToolContext，
        // @Tool 方法可通过 toolContext.getContext().get("agentChatContext") 获取

        ToolExecutionEvent startEvent = ToolExecutionEvent.start(toolName, toolInput);
        // 推送"开始"事件
        emitSse(startEvent);
        // 累积到上下文（用于持久化）
        ctx.getToolCalls().add(startEvent);

        try {
            // 执行实际工具
            String result = delegate.call(toolInput, toolContext);

            // ═══ 工具结果加工点 ═══
            // 可在返回模型前修改 result，例如：截断、过滤、格式化
            String modified = result;

            ToolExecutionEvent completeEvent = ToolExecutionEvent.complete(toolName, toolInput, modified, false);
            // 推送"完成"事件（使用修改后的结果，以便前端实时看到）
            emitSse(completeEvent);
            // 累积到上下文（用于持久化）
            ctx.getToolCalls().add(completeEvent);

            return modified;

        } catch (Exception e) {
            log.warn("[AgentToolCallbackWrapper] 工具执行失败: {}", toolName, e);
            ToolExecutionEvent errorEvent = ToolExecutionEvent.complete(toolName, toolInput, "错误: " + e.getMessage(), true);
            emitSse(errorEvent);
            ctx.getToolCalls().add(errorEvent);
            throw e;
        }
    }

    /**
     * 将 ToolExecutionEvent 序列化为 SSE 事件并推送
     */
    private void emitSse(ToolExecutionEvent event) {
        ctx.getEventPublisher().emit(ctx, AgentEvent.TOOL_EXECUTION, event);
    }
}
