/**
 * DeepseekChatMetadataHandler — DeepSeek 元数据处理器
 * <p>
 * 从 DeepSeek 的 NativeUsage 中提取更精确的缓存命中 token 数，
 * 构建 MetadataInfo 实体并存入 ChatContext，作为单一数据源
 * 同时供 SSE 事件推送和消息持久化使用。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.handler.ChatMetadataHandler;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import cc.wlizhi.eddie.common.util.PriceCalculator;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Order
@Component
public class DeepseekChatMetadataHandler implements ChatMetadataHandler {

    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "deepseek");
    }

    @Override
    public MetadataInfo buildMetadata(ChatContext ctx) {
        MetadataInfo.MetadataInfoBuilder builder = MetadataInfo.builder();

        ChatResponse lastResponse = ctx.getLastResponse();
        if (lastResponse != null) {
            ChatResponseMetadata metadata = lastResponse.getMetadata();
            Usage usage = metadata.getUsage();
            int promptTokens = usage.getPromptTokens();
            int completionTokens = usage.getCompletionTokens();
            int totalTokens = usage.getTotalTokens();

            // 通用缓存字段（fallback，使用引用类型避免 null 问题）
            Long cacheRead = usage.getCacheReadInputTokens();
            Long cacheWrite = usage.getCacheWriteInputTokens();
            Integer cacheReadTokens = cacheRead != null ? cacheRead.intValue() : null;
            Integer cacheWriteTokens = cacheWrite != null ? cacheWrite.intValue() : null;

            // DeepSeek 特有的 NativeUsage：更精确的缓存命中数
            Object nativeUsage = usage.getNativeUsage();
            if (nativeUsage instanceof DeepSeekApi.Usage deepSeekUsage) {
                DeepSeekApi.Usage.PromptTokensDetails details = deepSeekUsage.promptTokensDetails();
                if (details != null) {
                    cacheReadTokens = details.cachedTokens();
                }
            }

            builder.promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .cacheReadInputTokens(cacheReadTokens != null ? cacheReadTokens : 0)
                    .cacheWriteInputTokens(cacheWriteTokens != null ? cacheWriteTokens : 0);

            // 预估费用（含缓存折扣计算）
            ModelPricing pricing = ctx.getPricing();
            if (pricing != null) {
                double cost = PriceCalculator.calculate(
                        promptTokens, completionTokens,
                        cacheReadTokens != null ? cacheReadTokens : 0,
                        cacheWriteTokens != null ? cacheWriteTokens : 0,
                        pricing.getEffectiveInputPrice(), pricing.getEffectiveOutputPrice(),
                        pricing.getEffectiveCacheInputPrice(), pricing.getEffectiveCacheWriteInputPrice());
                builder.costEstimate(cost);
                builder.currency(pricing.getCurrency() != null ? pricing.getCurrency() : "");
            }
        }

        MetadataInfo info = builder.build();
        // 存入上下文，作为单一数据源供持久化使用
        ctx.setMetadata(info);
        return info;
    }
}
