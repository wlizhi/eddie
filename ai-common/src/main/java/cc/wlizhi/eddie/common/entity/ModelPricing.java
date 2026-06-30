/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型价格配置 — 每百万 token 单价
 * <p>
 * 从 provider.models JSON 中解析，包含标准 input/output 价格以及缓存命中折扣价。
 * cacheInputPrice 为可选字段（未来模型设置中可配置），没有值时退化到 inputPrice。
 */
@Getter
@Setter
public class ModelPricing {

    /**
     * input 单价（每百万 token），缓存未命中时的价格
     */
    private Double inputPrice;

    /**
     * output 单价（每百万 token）
     */
    private Double outputPrice;

    /**
     * 缓存命中时的 input 折扣单价（每百万 token）
     * 可为 null，此时退化到 inputPrice
     */
    private Double cacheInputPrice;

    /**
     * 缓存写入时的 input 价格（每百万 token）
     * 可为 null，此时退化到 inputPrice（与未命中同价）
     */
    private Double cacheWriteInputPrice;

    /**
     * 货币符号，如 ¥ / $
     */
    private String currency;

    /**
     * 获取缓存读取 input 价格，无折扣时回退到 inputPrice
     */
    public double getEffectiveCacheInputPrice() {
        return cacheInputPrice != null ? cacheInputPrice : (inputPrice != null ? inputPrice : 0.0);
    }

    /**
     * 获取缓存写入 input 价格，未设置时回退到 inputPrice
     */
    public double getEffectiveCacheWriteInputPrice() {
        return cacheWriteInputPrice != null ? cacheWriteInputPrice : (inputPrice != null ? inputPrice : 0.0);
    }

    /**
     * 获取有效的 input 价格（兜底 0）
     */
    public double getEffectiveInputPrice() {
        return inputPrice != null ? inputPrice : 0.0;
    }

    /**
     * 获取有效的 output 价格（兜底 0）
     */
    public double getEffectiveOutputPrice() {
        return outputPrice != null ? outputPrice : 0.0;
    }
}
