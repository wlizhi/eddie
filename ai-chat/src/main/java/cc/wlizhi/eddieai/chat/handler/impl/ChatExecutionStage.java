/**
 * ChatExecutionStage — 聊天流式执行器
 * <p>
 * 职责：使用构建好的 ChatClient 执行流式调用，并将最后一条 ChatResponse 写回 Context。
 * <p>
 * 此组件专注执行，不涉及 SSE 事件转换，后者由 SseEventTransformer 处理。
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatExecutionStage {

    /**
     * 执行流式聊天调用
     *
     * @param ctx 上下文，需包含已构建好的 ChatClient、SystemPrompt、UserMessage、ConversationId
     * @return ChatResponse 流
     */
    public Flux<ChatResponse> execute(ChatContext ctx) {
        return ctx.getChatClient().prompt()
                .system(ctx.getSystemPrompt())
                .user(ctx.getUserMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getConversationId()))
                .stream()
                .chatResponse()
                .doOnNext(ctx::setLastResponse);
    }
}
