/**
 * DefaultChatMetadataHandler — 默认元数据处理器
 * <p>
 * 构建通用的响应元数据（耗时、Token 用量等）。
 * 匹配所有 providerCode，作为 fallback 兜底。
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.handler.ChatMetadataHandler;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultChatMetadataHandler implements ChatMetadataHandler {

    @Override
    public boolean support(String providerCode) {
        return true;
    }

    @Override
    public Map<String, Object> buildMetadata(ChatContext ctx) {
        Map<String, Object> data = new LinkedHashMap<>();

        ChatResponse lastResponse = ctx.getLastResponse();
        if (lastResponse != null) {
            ChatResponseMetadata metadata = lastResponse.getMetadata();
            Usage usage = metadata.getUsage();
            if (usage.getPromptTokens() != null) data.put("promptTokens", usage.getPromptTokens());
            if (usage.getCompletionTokens() != null) data.put("completionTokens", usage.getCompletionTokens());
            if (usage.getTotalTokens() != null) data.put("totalTokens", usage.getTotalTokens());
        }

        return data;
    }
}
