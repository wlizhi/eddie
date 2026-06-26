package cc.wlizhi.eddie.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 费用计算工具类（基于 BigDecimal，避免浮点精度问题）
 * <p>
 * 模型价格配置为每百万 token 的单价（如 input_price: 1.0 表示每百万 input token ¥1.0），
 * 计算时：cost = (tokens / 1_000_000) × unitPrice
 */
public final class PriceCalculator {

    private static final BigDecimal MILLION = new BigDecimal(1_000_000);
    private static final int SCALE = 6;

    private PriceCalculator() {
    }

    /**
     * 计算预估费用（不含缓存折扣）
     *
     * @param promptTokens     prompt token 数
     * @param completionTokens completion token 数
     * @param inputPrice       input 单价（每百万 token）
     * @param outputPrice      output 单价（每百万 token）
     * @return 计算后的费用，保留 6 位小数
     */
    public static double calculate(int promptTokens, int completionTokens,
                                   double inputPrice, double outputPrice) {
        return calculate(promptTokens, completionTokens, 0,
                inputPrice, outputPrice, inputPrice);
    }

    /**
     * 计算预估费用（含缓存折扣）
     * <p>
     * 缓存命中部分按 cacheInputPrice 计价，未命中部分按 inputPrice 计价。
     * 任何参数为 null 时按 0 处理（兜底）。
     *
     * @param promptTokens     prompt token 数（含缓存命中部分）
     * @param completionTokens completion token 数
     * @param cacheReadTokens  缓存命中的 input token 数
     * @param inputPrice       input 单价（缓存未命中，每百万 token）
     * @param outputPrice      output 单价（每百万 token）
     * @param cacheInputPrice  缓存命中时的 input 折扣单价（每百万 token）
     * @return 计算后的费用，保留 6 位小数
     */
    public static double calculate(int promptTokens, int completionTokens,
                                   int cacheReadTokens,
                                   double inputPrice, double outputPrice,
                                   double cacheInputPrice) {
        int nonCacheTokens = Math.max(0, promptTokens - cacheReadTokens);

        BigDecimal nonCache = BigDecimal.valueOf(nonCacheTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputPrice));
        BigDecimal cache = BigDecimal.valueOf(cacheReadTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(cacheInputPrice));
        BigDecimal completion = BigDecimal.valueOf(completionTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputPrice));

        return nonCache.add(cache).add(completion)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
