/**
 * DefaultChatMetadataHandler — 默认元数据处理器
 * <p>
 * 构建通用的响应元数据（耗时、Token 用量等）。
 * 匹配所有 providerCode，作为 fallback 兜底。
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.handler.ChatMetadataHandler;
import cc.wlizhi.eddie.common.util.PriceCalculator;
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
            data.put("promptTokens", usage.getPromptTokens());
            data.put("completionTokens", usage.getCompletionTokens());
            data.put("totalTokens", usage.getTotalTokens());
            data.put("cacheReadInputTokens", usage.getCacheReadInputTokens());
            data.put("cacheWriteInputTokens", usage.getCacheWriteInputTokens());

            // 预估费用（单价为每百万 token，BigDecimal 精确计算）
            if (ctx.getInputPrice() != null) {
                double cost = PriceCalculator.calculate(
                        usage.getPromptTokens(), usage.getCompletionTokens(),
                        ctx.getInputPrice(), ctx.getOutputPrice());
                data.put("costEstimate", cost);
                data.put("currency", ctx.getCurrency());
            }
        }

        return data;
    }
}
