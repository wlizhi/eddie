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
     * 计算预估费用
     *
     * @param promptTokens     prompt token 数
     * @param completionTokens completion token 数
     * @param inputPrice       input 单价（每百万 token）
     * @param outputPrice      output 单价（每百万 token）
     * @return 计算后的费用，保留 6 位小数
     */
    public static double calculate(int promptTokens, int completionTokens,
                                   double inputPrice, double outputPrice) {
        BigDecimal prompt = BigDecimal.valueOf(promptTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputPrice));
        BigDecimal completion = BigDecimal.valueOf(completionTokens)
                .divide(MILLION, SCALE << 1, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputPrice));
        return prompt.add(completion)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
