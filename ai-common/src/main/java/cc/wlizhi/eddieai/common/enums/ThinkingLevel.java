package cc.wlizhi.eddieai.common.enums;

/**
 * 思考等级
 */
public enum ThinkingLevel {

    NONE("none", "无思考"),
    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高");

    private final String code;
    private final String label;

    ThinkingLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
