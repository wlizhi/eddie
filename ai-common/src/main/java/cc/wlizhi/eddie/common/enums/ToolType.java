package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * 工具类型枚举
 * <p>
 * BUILT_IN — 内置工具（通过 {@code BuiltInToolProvider} Bean 注册）
 * MCP — MCP 工具（通过 MCP 客户端连接获取）
 */
@Getter
public enum ToolType {

    BUILT_IN("内置工具"),
    MCP("MCP 工具");

    private final String label;

    ToolType(String label) {
        this.label = label;
    }

    /**
     * 根据 name 反查枚举（忽略大小写）
     */
    public static ToolType fromCode(String code) {
        if (code == null) return null;
        for (ToolType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
