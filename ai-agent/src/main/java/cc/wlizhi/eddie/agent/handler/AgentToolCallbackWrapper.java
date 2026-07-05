/**
 * AgentToolCallbackWrapper — 智能体工具回调包装器
 * <p>
 * 在智能体循环中，将 ToolCallback 的执行状态通过 SSE 推送到前端。
 * 与 ai-chat 模块的 ToolCallbackWrapper 不同，此处直接使用 FluxSink
 * 推 ServerSentEvent，避免引入 Sinks.Many 的中间层。
 * <p>
 * 可在 call() 中修改工具执行结果再返回给模型（工具结果加工点）。
 *
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.lang.Nullable;
import reactor.core.publisher.FluxSink;

@Slf4j
public class AgentToolCallbackWrapper implements ToolCallback {

    private final ToolCallback delegate;
    private final FluxSink<ServerSentEvent<String>> sink;
    private final ObjectMapper objectMapper;

    public AgentToolCallbackWrapper(ToolCallback delegate,
                                    FluxSink<ServerSentEvent<String>> sink,
                                    ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.sink = sink;
        this.objectMapper = objectMapper;
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

        // 推送"开始"事件
        emitSse(ToolExecutionEvent.start(toolName, toolInput));

        try {
            // 执行实际工具
            String result = delegate.call(toolInput, toolContext);

            // ═══ 工具结果加工点 ═══
            // 可在返回模型前修改 result，例如：截断、过滤、格式化
            String modified = result;

            // 推送"完成"事件（使用修改后的结果，以便前端实时看到）
            emitSse(ToolExecutionEvent.complete(toolName, toolInput, modified, false));

            return modified;

        } catch (Exception e) {
            log.warn("[AgentToolCallbackWrapper] 工具执行失败: {}", toolName, e);
            emitSse(ToolExecutionEvent.complete(toolName, toolInput, "错误: " + e.getMessage(), true));
            throw e;
        }
    }

    /**
     * 将 ToolExecutionEvent 序列化为 SSE 事件并推送
     */
    private void emitSse(ToolExecutionEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            sink.next(ServerSentEvent.<String>builder()
                    .event("tool_execution")
                    .data(json)
                    .build());
        } catch (JsonProcessingException e) {
            log.debug("[AgentToolCallbackWrapper] 序列化工具事件失败", e);
        }
    }
}
