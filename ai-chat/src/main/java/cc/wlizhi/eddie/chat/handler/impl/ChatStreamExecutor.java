/**
 * ChatStreamExecutor — 聊天流式执行器
 * <p>
 * 职责：使用构建好的 ChatClient 执行流式调用，并将最后一条 ChatResponse 写回 Context。
 * <p>
 * 此组件专注执行，不涉及 SSE 事件转换，后者由 ChatSseTransformer 处理。
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.common.util.PromptVariableResolver;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatStreamExecutor {

    @Resource
    private PromptVariableResolver promptVariableResolver;

    /**
     * 执行流式聊天调用
     *
     * @param ctx 上下文，需包含已构建好的 ChatClient、SystemPrompt、UserMessage、ConversationId
     * @return ChatResponse 流
     */
    public Flux<ChatResponse> execute(ChatContext ctx) {
        return ctx.getChatClient().prompt()
                .system(promptVariableResolver.resolve(ctx.getAssistant().getSystemPrompt()))
                .user(ctx.getUserMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId()))
                .stream()
                .chatResponse()
                .doOnNext(ctx::setLastResponse);
    }
}
