/**
 * DefaultChatThinkingHandler — 默认思考内容处理器
 * <p>
 * 处理 DeepSeek 模型的 reasoning_content 提取。
 * 从 DeepseekChatPolicy 迁移至此，使 ChatPolicy 职责单一。
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.handler.ChatThinkingHandler;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DefaultChatThinkingHandler implements ChatThinkingHandler {

    @Override
    public boolean support(String providerCode) {
        return Set.of("deepseek", "dashscope").contains(providerCode);
    }

    @Override
    public String extractThinking(ChatResponse response, ChatContext ctx) {
        // 从 DeepSeek 的 ChatResponse 中提取 reasoning_content
        String reasoning = response.getResults().stream()
                .map(Generation::getOutput)
                .map(msg -> {
                    if (msg instanceof DeepSeekAssistantMessage deepSeekAssistantMessage) {
                        return deepSeekAssistantMessage.getReasoningContent();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        return ObjectUtils.isEmpty(reasoning) ? null : reasoning;
    }
}
