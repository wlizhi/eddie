/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

/**
 * PLAN 模式流式响应处理器
 * <p>
 * 规划模式：在流开始前推送 "plan_start" 事件，流结束后推送 "plan_complete" 事件。
 * 由基类处理 thinking + answer 推送。
 */
@Component
public class PlanResponseStreamProcessor extends AbstractStreamProcessor {

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.PLAN == agentMode;
    }

    @Override
    protected void beforeStream(AgentChatContext ctx) {
        ctx.getSink().next(ServerSentEvent.<String>builder()
                .event("plan_start")
                .data("规划开始")
                .build());
    }

    @Override
    protected void afterStream(AgentChatContext ctx) {
        ctx.getSink().next(ServerSentEvent.<String>builder()
                .event("plan_complete")
                .data("规划完成")
                .build());
    }
}
