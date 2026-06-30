/**
 * DefaultChatMetadataHandler — 默认元数据处理器
 * <p>
 * 构建通用的响应元数据（耗时、Token 用量等）。
 * 返回 MetadataInfo 实体并存入 ChatContext，作为单一数据源
 * 同时供 SSE 事件推送和消息持久化使用。
 * <p>
 * 匹配所有 providerCode，作为 fallback 兜底。
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order
@Component
public class DefaultChatMetadataHandler implements ChatMetadataHandler {

    @Override
    public boolean support(String providerCode) {
        return true;
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

            // 读取缓存字段
            Long cacheRead = usage.getCacheReadInputTokens();
            Long cacheWrite = usage.getCacheWriteInputTokens();
            int cacheReadTokens = cacheRead != null ? cacheRead.intValue() : 0;
            int cacheWriteTokens = cacheWrite != null ? cacheWrite.intValue() : 0;

            builder.promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .cacheReadInputTokens(cacheReadTokens)
                    .cacheWriteInputTokens(cacheWriteTokens);

            // 预估费用（含缓存折扣计算）
            ModelPricing pricing = ctx.getPricing();
            if (pricing != null) {
                double cost = PriceCalculator.calculate(
                        promptTokens, completionTokens, cacheReadTokens, cacheWriteTokens,
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
