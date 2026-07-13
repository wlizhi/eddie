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
 * SUB_TASK 模式流式响应处理器
 * <p>
 * 子任务模式：流开始前推送 "sub_task_start" 事件，流结束后推送 "sub_task_complete" 事件。
 * 由基类处理思考内容和回答推送。
 */
@Component
public class SubTaskResponseStreamProcessor extends AbstractStreamProcessor {

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.SUB_TASK == agentMode;
    }

    @Override
    protected void beforeStream(AgentChatContext ctx) {
        ctx.getEvent().getSink().next(ServerSentEvent.<String>builder()
                .event("sub_task_start")
                .data("子任务开始")
                .build());
    }

    @Override
    protected void afterStream(AgentChatContext ctx) {
        // 先执行基类通用逻辑（token 提取 + 持久化 + metadata 推送）
        super.afterStream(ctx);
        ctx.getEvent().getSink().next(ServerSentEvent.<String>builder()
                .event("sub_task_complete")
                .data("子任务完成")
                .build());
    }
}
