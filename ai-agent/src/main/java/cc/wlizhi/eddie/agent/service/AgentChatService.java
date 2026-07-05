/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service;

import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 智能体业务接口
 */
public interface AgentChatService {

    /**
     * 发送 Agent 聊天消息，返回 SSE 流式响应
     * <p>
     * 智能体将接管执行流程，自动进行任务规划、工具调用、多轮迭代。
     *
     * @param request 请求参数
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> chat(AgentChatRequest request);

    void stop(Long messageId, String mode);
}
