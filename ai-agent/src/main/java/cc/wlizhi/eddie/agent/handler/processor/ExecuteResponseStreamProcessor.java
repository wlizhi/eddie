/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

/**
 * EXECUTE 模式流式响应处理器
 * <p>
 * 执行模式：在每条 ChatResponse 中检查是否有工具调用（tool_calls），
 * 若有则推送 "tool_call" 事件告知前端。
 * 由基类处理 thinking + answer 推送。
 */
@Component
public class ExecuteResponseStreamProcessor extends AbstractStreamProcessor {

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.EXECUTE == agentMode;
    }

    @Override
    protected void beforeStream(AgentChatContext ctx) {
        ctx.getSink().next(ServerSentEvent.<String>builder()
                .event("execute_start")
                .data("开始执行")
                .build());
    }

    @Override
    protected void handleCustomEvent(AgentChatContext ctx, ChatResponse response) {
        // 检测是否有工具调用，若有则推送 tool_call 事件
        if (response.getResult() != null) {
            if (!response.getResult().getOutput().getToolCalls().isEmpty()) {
                ctx.getSink().next(ServerSentEvent.<String>builder()
                        .event("tool_call")
                        .data("工具调用中...")
                        .build());
            }
        }
    }

    @Override
    protected void afterStream(AgentChatContext ctx) {
        // 先执行基类通用逻辑（token 提取 + 持久化 + metadata 推送）
        super.afterStream(ctx);
        ctx.getSink().next(ServerSentEvent.<String>builder()
                .event("execute_complete")
                .data("执行完成")
                .build());
    }
}
