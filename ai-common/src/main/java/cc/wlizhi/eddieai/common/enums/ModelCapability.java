package cc.wlizhi.eddieai.common.enums;

/**
 * 模型能力标签
 * <p>
 * 用于标识模型支持的功能特性，前端根据此标签渲染类型图标。
 * 能力信息存储在 model_provider.models JSON 的 capabilities 字段中。
 */
public enum ModelCapability {

    VISION("vision", "视觉"),
    WEB_SEARCH("web_search", "联网"),
    REASONING("reasoning", "推理"),
    FUNCTION_CALLING("function_calling", "工具"),
    RERANK("rerank", "重排"),
    EMBEDDING("embedding", "嵌入");

    private final String code;
    private final String label;

    ModelCapability(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据 code 反查枚举
     */
    public static ModelCapability fromCode(String code) {
        for (ModelCapability cap : values()) {
            if (cap.code.equals(code)) {
                return cap;
            }
        }
        return null;
    }
}
