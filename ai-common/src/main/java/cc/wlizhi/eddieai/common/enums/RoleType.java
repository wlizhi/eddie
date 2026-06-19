package cc.wlizhi.eddieai.common.enums;

/**
 * 角色类型：助手 / 智能体
 */
public enum RoleType {

    ASSISTANT("assistant", "助手"),
    AGENT("agent", "智能体");

    private final String code;
    private final String label;

    RoleType(String code, String label) {
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
