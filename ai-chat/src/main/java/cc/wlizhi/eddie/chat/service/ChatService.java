/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.chat.service;

import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 聊天业务接口
 */
public interface ChatService {

    /**
     * 发送聊天消息，返回 SSE 流
     * <p>
     * 事件类型：
     * <ul>
     *   <li>{@code event: thinking} — 模型思考内容（DeepSeek reasoning_content）</li>
     *   <li>{@code event: answer} — 模型回答内容</li>
     *   <li>{@code event: metadata} — 回答完毕后的元数据（耗时、token 统计等）</li>
     * </ul>
     */
    Flux<ServerSentEvent<String>> chat(ChatRequest request);
}
