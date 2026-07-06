package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 响应流处理器路由
 * <p>
 * 根据当前 {@link cc.wlizhi.eddie.agent.entity.dto.AgentIteratorState#getAgentMode()}
 * 选择合适的 {@link ResponseStreamProcessor} 处理响应流。
 *
 * @author Eddie
 * {@code @date} 2026-07-06
 */
@Component
public class ResponseStreamProcessorRouter {

    @Resource
    private List<ResponseStreamProcessor> processors;

    /**
     * 处理 ChatResponse 流，向前端推送 SSE 事件
     *
     * @param ctx         当前请求上下文
     * @param requestSpec 已构建好的 ChatClient 请求体
     */
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        for (ResponseStreamProcessor processor : processors) {
            if (processor.support(ctx.getIteratorState().getAgentMode())) {
                processor.process(ctx, requestSpec);
                return;
            }
        }
        throw new NotFoundException("找不到匹配的 ResponseStreamProcessor，当前模式: "
                + ctx.getIteratorState().getAgentMode());
    }
}
