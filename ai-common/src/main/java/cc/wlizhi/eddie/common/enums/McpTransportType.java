package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * MCP 传输方式枚举
 * <p>
 * STDIO — 标准输入输出传输（通过子进程通信）
 * SSE — Server-Sent Events 传输（已弃用，保留兼容）
 * STREAMABLE_HTTP — Streamable HTTP 传输（推荐）
 * <p>
 * 枚举 name() 直接作为数据库中 {@code transport_type} 的存储值，
 * 如 {@code McpTransportType.STREAMABLE_HTTP.name()} → {@code "STREAMABLE_HTTP"}。
 */
@Getter
public enum McpTransportType {

    STDIO("标准输入输出"),
    SSE("Server-Sent Events"),
    STREAMABLE_HTTP("Streamable HTTP");

    private final String label;

    McpTransportType(String label) {
        this.label = label;
    }

    /**
     * 根据 code 反查枚举（忽略大小写）
     */
    public static McpTransportType fromCode(String code) {
        if (code == null) return null;
        for (McpTransportType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
