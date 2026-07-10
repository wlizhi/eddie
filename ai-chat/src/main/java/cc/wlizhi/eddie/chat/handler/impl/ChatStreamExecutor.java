/**
 * ChatStreamExecutor — 聊天流式执行器
 * <p>
 * 职责：使用构建好的 ChatClient 执行流式调用，并将最后一条 ChatResponse 写回 Context。
 * <p>
 * 此组件专注执行，不涉及 SSE 事件转换，后者由 ChatSseTransformer 处理。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.util.PromptVariableResolver;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatStreamExecutor {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamExecutor.class);

    @Resource
    private PromptVariableResolver promptVariableResolver;

    @Resource
    private EventRegistry chatEventRegistry;

    /**
     * 执行流式聊天调用
     * <p>
     * 每次 emit 前检查注册表中是否有停止事件，有则提前终止流。
     *
     * @param ctx 上下文，需包含已构建好的 ChatClient、SystemPrompt、UserMessage、ConversationId
     * @return ChatResponse 流
     */
    public Flux<ChatResponse> execute(ChatContext ctx) {
        String registryKey = EventRegistry.key("STOP", String.valueOf(ctx.getPlaceholderMsgId()));
        return ctx.getChatClient().prompt()
                .system(promptVariableResolver.resolve(ctx.getAssistant().getSystemPrompt()))
                .user(ctx.getUserMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId())
                        .param("providerId", ctx.getProvider().getId())
                        .param("modelCode", ctx.getOriginalRequest().getModelId()))
                .stream()
                .chatResponse()
                .doOnNext(ctx::setLastResponse)
                .takeWhile(__ -> {
                    String mode = chatEventRegistry.get(registryKey);
                    if (mode != null) {
                        log.info("检测到停止事件 ({}), 终止流: userMessageId={}", mode, ctx.getUserMessageId());
                        ctx.setInterrupted(true);
                        ctx.getAttributes().put("cancelMode", mode);
                        return false;
                    }
                    return true;
                });
    }
}
