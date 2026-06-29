package cc.wlizhi.eddie.common.enums;

/**
 * MCP 服务来源类型枚举
 * <p>
 * BUILT_IN — 内置工具，通过 Java {@code @Tool} 方法注册，无需外部 MCP Server 连接
 * USER     — 用户自定义 MCP 服务，通过 STDIO/SSE/Streamable HTTP 协议接入
 * PROVIDER — 第三方服务商预置 MCP（如阿里云百炼），需配置 API Key 等认证信息
 */
public enum McpSourceType {

    BUILT_IN,
    USER,
    PROVIDER;

    /**
     * 根据 code 反查枚举（忽略大小写），null 或无法匹配时返回 null
     */
    public static McpSourceType fromCode(String code) {
        if (code == null) return null;
        for (McpSourceType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 是否系统预置（不可删除）
     */
    public boolean isSystem() {
        return this != USER;
    }
}
