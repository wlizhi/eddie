/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.common.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 模型存储项 — provider.models JSON 数组中的单个元素
 * <p>
 * 对应数据库 ModelProviderEntity.models 字段中 JSON 数组的每个元素，
 * 使用 {@code @JsonProperty} 将 snake_case JSON 字段映射为 camelCase Java 属性。
 * <p>
 * 可直接通过 Jackson {@code ObjectMapper} 反序列化/序列化，替代 {@code List<Map<String, Object>>} 的硬编码操作。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelJsonItem {

    /**
     * 模型 ID，对应 JSON 中的 id
     */
    @JsonProperty("id")
    private String id;

    /**
     * 模型展示名称，对应 JSON 中的 name
     */
    @JsonProperty("name")
    private String name;

    /**
     * 对象类型，对应 JSON 中的 object，通常为 "model"
     */
    @JsonProperty("object")
    private String object;

    /**
     * 模型归属方，对应 JSON 中的 owned_by
     */
    @JsonProperty("owned_by")
    private String ownedBy;

    /**
     * 模型能力标签列表，对应 JSON 中的 capabilities，如 ["function_calling", "vision"]
     */
    @JsonProperty("capabilities")
    private List<String> capabilities;

    /**
     * 币种符号，对应 JSON 中的 currency，如 ¥ / $
     */
    @JsonProperty("currency")
    private String currency;

    /**
     * 输入价格（每百万 token），对应 JSON 中的 input_price
     */
    @JsonProperty("input_price")
    private Double inputPrice;

    /**
     * 输出价格（每百万 token），对应 JSON 中的 output_price
     */
    @JsonProperty("output_price")
    private Double outputPrice;

    /**
     * 缓存命中价格（每百万 token），对应 JSON 中的 cache_input_price
     */
    @JsonProperty("cache_input_price")
    private Double cacheInputPrice;

    /**
     * 缓存写入价格（每百万 token），对应 JSON 中的 cache_write_input_price
     */
    @JsonProperty("cache_write_input_price")
    private Double cacheWriteInputPrice;

    /**
     * 调用最小间隔（秒），对应 JSON 中的 call_interval_sec，null 表示不限制
     */
    @JsonProperty("call_interval_sec")
    private Integer callIntervalSec;

    /**
     * 判断是否具备指定能力
     *
     * @param capability 能力名称（如 "function_calling"）
     * @return 如果 capabilities 包含该能力返回 true
     */
    public boolean hasCapability(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }

    /**
     * 获取有效的 input 价格，兜底 0
     */
    public double getEffectiveInputPrice() {
        return inputPrice != null ? inputPrice : 0.0;
    }

    /**
     * 获取有效的 output 价格，兜底 0
     */
    public double getEffectiveOutputPrice() {
        return outputPrice != null ? outputPrice : 0.0;
    }

    /**
     * 获取缓存读取 input 价格，无折扣时回退到 inputPrice
     */
    public double getEffectiveCacheInputPrice() {
        return cacheInputPrice != null ? cacheInputPrice : getEffectiveInputPrice();
    }

    /**
     * 获取缓存写入 input 价格，未设置时回退到 inputPrice
     */
    public double getEffectiveCacheWriteInputPrice() {
        return cacheWriteInputPrice != null ? cacheWriteInputPrice : getEffectiveInputPrice();
    }
}
