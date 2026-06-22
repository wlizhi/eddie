package cc.wlizhi.eddieai.settings.entity.response;

import cc.wlizhi.eddieai.common.enums.ModelCapability;

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
     * 模型能力标签列表，如 [vision, function_calling, reasoning]
     */
    private List<ModelCapability> capabilities;

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

    public List<ModelCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<ModelCapability> capabilities) {
        this.capabilities = capabilities;
    }
}
