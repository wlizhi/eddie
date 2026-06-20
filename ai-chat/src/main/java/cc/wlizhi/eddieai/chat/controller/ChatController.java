package cc.wlizhi.eddieai.chat.controller;

import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 聊天接口
 * <p>
 * 使用 SSE (Server-Sent Events) 实现打字机效果。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 发送聊天消息，返回 SSE 流式响应
     * <p>
     * 事件类型：
     * <ul>
     *   <li><code>event: thinking</code> — 模型思考内容</li>
     *   <li><code>event: answer</code> — 模型回答内容</li>
     *   <li><code>event: metadata</code> — 回答完毕后的元数据</li>
     * </ul>
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> send(@Validated @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
