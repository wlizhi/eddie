/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.event.payload.ToolExecutionPayload;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.handler.AbstractApprovalInterceptor;
import org.springframework.ai.tool.ToolCallback;

/**
 * 智能体工具审批拦截器
 * <p>
 * 包装原始 {@link ToolCallback}，继承 {@link AbstractApprovalInterceptor}，
 * 使用 {@link AgentEventPublisher} 通过 SSE 向前端发射 {@code pending_approval} 事件。
 * <p>
 * 调用方在包装前已按 {@code enabled == 2}（PENDING_APPROVAL）过滤，无需再传入
 * {@code ToolDefinitionEntity} 检查启用状态。
 */
public class AgentApprovalInterceptor extends AbstractApprovalInterceptor {

    private final AgentEventPublisher publisher;
    private final AgentChatContext ctx;

    /**
     * @param delegate      原始工具回调
     * @param eventRegistry 事件注册表
     * @param publisher     智能体事件发布器
     * @param ctx           智能体上下文
     */
    public AgentApprovalInterceptor(ToolCallback delegate, AgentChatContext ctx) {
        super(delegate, ctx.getEventRegistry(),
                "agent",
                ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null,
                resolveStepId(ctx));
        this.publisher = ctx.getEventPublisher();
        this.ctx = ctx;
    }

    @Override
    protected void emitPendingApproval(String toolName, String toolInput) {
        try {
            ToolExecutionPayload payload = new ToolExecutionPayload(
                    msgId, stepId, ctx.getCurrentStep(),
                    toolName, "pending_approval",
                    toolInput, null, false,
                    getToolCallSequence()
            );
            if (publisher != null) {
                publisher.emit(ctx, AgentEvent.TOOL_EXECUTION, ApiResult.success(payload));
            }
        } catch (Exception e) {
            log.warn("发射 pending_approval SSE 事件失败: toolName={}", toolName, e);
        }
    }

    /**
     * 从 AgentChatContext 解析当前步骤 ID
     */
    private static Long resolveStepId(AgentChatContext ctx) {
        if (ctx == null) return null;
        var stepCtx = ctx.getStepStreamContext();
        if (stepCtx != null && stepCtx.getStepId() != null) {
            return stepCtx.getStepId();
        }
        return null;
    }

    @Override
    protected String getStopEventKey() {
        if (msgId == null) return null;
        return EventRegistry.key(AgentEvent.STOP_MSG.name().toLowerCase(), msgId.toString());
    }
}
