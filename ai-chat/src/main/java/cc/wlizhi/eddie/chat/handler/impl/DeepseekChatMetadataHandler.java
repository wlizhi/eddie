/**
 * DefaultChatMetadataHandler — 默认元数据处理器
 * <p>
 * 构建通用的响应元数据（耗时、Token 用量等）。
 * 匹配所有 providerCode，作为 fallback 兜底。
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.handler.ChatMetadataHandler;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import cc.wlizhi.eddie.common.util.PriceCalculator;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Order
@Component
public class DeepseekChatMetadataHandler implements ChatMetadataHandler {

    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "deepseek");
    }

    @Override
    public Map<String, Object> buildMetadata(ChatContext ctx) {
        Map<String, Object> data = new LinkedHashMap<>();

        ChatResponse lastResponse = ctx.getLastResponse();
        if (lastResponse != null) {
            ChatResponseMetadata metadata = lastResponse.getMetadata();
            Usage usage = metadata.getUsage();
            int promptTokens = usage.getPromptTokens();
            int completionTokens = usage.getCompletionTokens();
            data.put("promptTokens", promptTokens);
            data.put("completionTokens", completionTokens);
            data.put("totalTokens", usage.getTotalTokens());


            Long cacheRead = usage.getCacheReadInputTokens();
            Long cacheWrite = usage.getCacheWriteInputTokens();
            int cacheReadTokens = cacheRead != null ? cacheRead.intValue() : 0;
            int cacheWriteTokens = cacheWrite != null ? cacheWrite.intValue() : 0;
            ctx.setCacheReadInputTokens(cacheReadTokens);
            ctx.setCacheWriteInputTokens(cacheWriteTokens);
            data.put("cacheReadInputTokens", cacheReadTokens);
            data.put("cacheWriteInputTokens", cacheWriteTokens);

            // 读取缓存字段并写入上下文中（用于后处理器持久化）
            Object nativeUsage = usage.getNativeUsage();
            if (nativeUsage instanceof DeepSeekApi.Usage deepSeekUsage) {
                DeepSeekApi.Usage.PromptTokensDetails details = deepSeekUsage.promptTokensDetails();
                if (details != null) {
                    data.put("cacheReadInputTokens", details.cachedTokens());
                }
            }

            // 预估费用（单价为每百万 token，BigDecimal 精确计算）
            ModelPricing pricing = ctx.getPricing();
            if (pricing != null) {
                double cost = PriceCalculator.calculate(
                        promptTokens, completionTokens, cacheReadTokens, cacheWriteTokens,
                        pricing.getEffectiveInputPrice(), pricing.getEffectiveOutputPrice(),
                        pricing.getEffectiveCacheInputPrice(), pricing.getEffectiveCacheWriteInputPrice());
                data.put("costEstimate", cost);
                data.put("currency", pricing.getCurrency() != null ? pricing.getCurrency() : "");
            }
        }
        return data;
    }
}
