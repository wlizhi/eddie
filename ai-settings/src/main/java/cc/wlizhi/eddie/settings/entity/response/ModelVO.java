/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.settings.entity.response;

import cc.wlizhi.eddie.common.enums.ModelCapability;

import java.util.List;

/**
 * 模型信息响应 VO（接口2：根据服务商 code 获取模型列表）
 */
public class ModelVO {

    /** 对应 JSON 中的 id 字段 */
    private String code;

    /** 对应 JSON 中的 object 字段 */
    private String object;

    /** 对应 JSON 中的 owned_by 字段 */
    private String ownedBy;

    /**
     * 对应 JSON 中的 created 字段（Unix 时间戳）
     */
    private Long created;

    /**
     * 模型能力标签列表，如 [vision, function_calling, reasoning]
     */
    private List<ModelCapability> capabilities;

    /**
     * 币种，如 USD、CNY
     */
    private String currency;

    /**
     * 输入价格，每百万 token
     */
    private Double inputPrice;

    /**
     * 输出价格，每百万 token
     */
    private Double outputPrice;

    /**
     * 缓存命中价格，每百万 token
     */
    private Double cacheInputPrice;

    /**
     * 缓存写入价格，每百万 token
     */
    private Double cacheWriteInputPrice;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public List<ModelCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<ModelCapability> capabilities) {
        this.capabilities = capabilities;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getInputPrice() {
        return inputPrice;
    }

    public void setInputPrice(Double inputPrice) {
        this.inputPrice = inputPrice;
    }

    public Double getOutputPrice() {
        return outputPrice;
    }

    public void setOutputPrice(Double outputPrice) {
        this.outputPrice = outputPrice;
    }

    public Double getCacheInputPrice() {
        return cacheInputPrice;
    }

    public void setCacheInputPrice(Double cacheInputPrice) {
        this.cacheInputPrice = cacheInputPrice;
    }

    public Double getCacheWriteInputPrice() {
        return cacheWriteInputPrice;
    }

    public void setCacheWriteInputPrice(Double cacheWriteInputPrice) {
        this.cacheWriteInputPrice = cacheWriteInputPrice;
    }
}
