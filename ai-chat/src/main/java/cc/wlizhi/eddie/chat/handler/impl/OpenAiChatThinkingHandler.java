/**
 * OpenAiChatThinkingHandler — OpenAI 兼容思考内容处理器（兜底）
 * <p>
 * 从 AssistantMessage.metadata 中提取 reasoningContent。
 * 作为 fallback 兜底，匹配所有未被其他 ChatThinkingHandler 处理的 providerCode。
 * OpenAI 兼容的服务商（如 OpenAI、部分第三方）会将推理内容存入 AssistantMessage 的 metadata，
 * key 为 "reasoningContent"。
 * <p>
 * 注意：DeepSeek 使用独立子类 DeepSeekAssistantMessage 的 reasoningContent 字段，
 * 由 DefaultChatThinkingHandler 处理，不会走到此兜底。
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.handler.ChatThinkingHandler;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Order
public class OpenAiChatThinkingHandler implements ChatThinkingHandler {

    /**
     * OpenAI ChatModel 将 reasoning_content 存入 AssistantMessage metadata 的 key
     *
     * @see org.springframework.ai.openai.OpenAiChatModel
     */
    private static final String REASONING_CONTENT_KEY = "reasoningContent";

    @Override
    public boolean support(String providerCode) {
        // 作为兜底，匹配所有未被其他 Handler 处理的 providerCode
        return true;
    }

    @Override
    public String extractThinking(ChatResponse response, ChatContext ctx) {
        if (response == null || response.getResults() == null) {
            return null;
        }

        String reasoning = response.getResults().stream()
                .map(Generation::getOutput)
                .filter(msg -> msg instanceof AssistantMessage)
                .map(msg -> {
                    Object val = msg.getMetadata().get(REASONING_CONTENT_KEY);
                    return (val instanceof String str && !str.isEmpty()) ? str : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining());

        return ObjectUtils.isEmpty(reasoning) ? null : reasoning;
    }
}
