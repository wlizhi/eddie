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
        return calculate(promptTokens, completionTokens, 0, 0,
                inputPrice, outputPrice, inputPrice, inputPrice);
    }

    /**
     * 计算预估费用（含缓存折扣）
     * <p>
     * prompt_tokens = cacheReadTokens + cacheWriteTokens + 其他未缓存部分。
     * 各部分按各自价格计价，兜底逻辑：null/0 时按 0 处理。
     * <ul>
     *   <li>缓存读取：按 cacheInputPrice 计价</li>
     *   <li>缓存写入：按 cacheWriteInputPrice 计价</li>
     *   <li>其余 input：按 inputPrice 计价</li>
     *   <li>output：按 outputPrice 计价</li>
     * </ul>
     *
     * @param promptTokens          prompt token 数（含缓存命中/写入部分）
     * @param completionTokens      completion token 数
     * @param cacheReadTokens       缓存读取的 input token 数（cache hit）
     * @param cacheWriteTokens      缓存写入的 input token 数（首次缓存，cache write）
     * @param inputPrice            input 单价（缓存未命中，每百万 token）
     * @param outputPrice           output 单价（每百万 token）
     * @param cacheInputPrice       缓存命中时的 input 折扣单价（每百万 token）
     * @param cacheWriteInputPrice  缓存写入时的 input 价格（每百万 token）
     * @return 计算后的费用，保留 6 位小数
     */
    public static double calculate(int promptTokens, int completionTokens,
                                   int cacheReadTokens, int cacheWriteTokens,
                                   double inputPrice, double outputPrice,
                                   double cacheInputPrice, double cacheWriteInputPrice) {
        int remainingTokens = Math.max(0, promptTokens - cacheReadTokens - cacheWriteTokens);

        BigDecimal remaining = BigDecimal.valueOf(remainingTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputPrice));
        BigDecimal cacheRead = BigDecimal.valueOf(cacheReadTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(cacheInputPrice));
        BigDecimal cacheWrite = BigDecimal.valueOf(cacheWriteTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(cacheWriteInputPrice));
        BigDecimal completion = BigDecimal.valueOf(completionTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputPrice));

        return remaining.add(cacheRead).add(cacheWrite).add(completion)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
