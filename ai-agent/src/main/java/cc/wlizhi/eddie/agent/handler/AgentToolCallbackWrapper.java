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
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.exception.SwitchModeToPlanException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import java.util.Objects;

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

        // 工具执行前检查用户是否已点击停止
        if (isStopRequested()) {
            log.info("用户已停止，跳过工具执行: {}", toolName);
            throw new UserStopException();
        }

        try {
            // 执行实际工具
            String result = delegate.call(toolInput, toolContext);

            // 工具执行后再次检查用户是否已点击停止
            if (isStopRequested()) {
                log.info("用户已停止，丢弃工具结果: {}", toolName);
                throw new UserStopException();
            }
            // 切换至计划模式执行后，退出当前对话
            checkSwitchToPlan(result);

            // ═══ 工具结果加工点 ═══
            // 三层截断，互不影响：
            //   模型级 → 返回给 LLM（上限最高）
            //   SSE 级  → 推送给前端渲染（上限中等）
            //   存储级  → 持久化到数据库（上限最低）
            String modelResult = result;
            int modelMaxLen = ctx.getToolResultModelMaxLength();
            if (modelMaxLen > 0 && modelResult != null && modelResult.length() > modelMaxLen) {
                modelResult = modelResult.substring(0, modelMaxLen) + "\n\n...（工具结果已截断，更多内容请参考原始数据）";
            }

            int sseMaxLen = ctx.getToolCallMaxLength();
            String sseResult = modelResult;
            if (sseMaxLen > 0 && sseResult != null && sseResult.length() > sseMaxLen) {
                sseResult = sseResult.substring(0, sseMaxLen) + "...（已截断）";
            }

            int storeMaxLen = ctx.getToolCallStoreMaxLength();
            String storeResult = modelResult;
            if (storeMaxLen > 0 && storeResult != null && storeResult.length() > storeMaxLen) {
                storeResult = storeResult.substring(0, storeMaxLen) + "...（已截断）";
            }

            // SSE 事件使用前端渲染截断后的结果
            ToolExecutionEvent sseEvent = ToolExecutionEvent.complete(toolName, toolInput, sseResult, false);
            emitSse(sseEvent);
            // 存储使用持久化截断后的结果
            ToolExecutionEvent storeEvent = ToolExecutionEvent.complete(toolName, toolInput, storeResult, false);
            ctx.getToolCalls().add(storeEvent);

            return modelResult;

        } catch (UserStopException | SwitchModeToPlanException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AgentToolCallbackWrapper] 工具执行失败: {}", toolName, e);
            ToolExecutionEvent errorEvent = ToolExecutionEvent.complete(toolName, toolInput, "错误: " + e.getMessage(), true);
            emitSse(errorEvent);
            ctx.getToolCalls().add(errorEvent);
            throw e;
        }
    }

    private void checkSwitchToPlan(String result) {
        try {
            ApiResult<String> apiResult = ctx.getObjectMapper().readValue(result, new TypeReference<>() {
            });
            if (apiResult != null && Objects.equals(apiResult.getData(), AgentEvent.SWITCH_MODE_PLAN.name())) {
                throw new SwitchModeToPlanException();
            }
        } catch (JsonProcessingException ignored) {
        }
    }

    /**
     * 将 ToolExecutionEvent 序列化为 SSE 事件并推送
     */
    private void emitSse(ToolExecutionEvent event) {
        ctx.getEventPublisher().emit(ctx, AgentEvent.TOOL_EXECUTION, event);
    }

    /**
     * 检查用户是否已点击停止回答，在工具执行前后调用，
     * 消除工具执行期间停止检测的盲区。
     */
    private boolean isStopRequested() {
        EventRegistry registry = ctx.getEventRegistry();
        if (registry == null || ctx.getAgentMsg() == null) {
            return false;
        }
        Long msgId = ctx.getAgentMsg().getId();

        String stopKey = EventRegistry.key(
                AgentEvent.STOP_MSG.name().toLowerCase(), msgId.toString());
        String stopVal = registry.get(stopKey);

        if (Objects.equals(stopVal, AgentEvent.STOP_MSG.name().toLowerCase())) {
            log.info("用户点击停止回答");
            return true;
        }
        return false;
    }
}
