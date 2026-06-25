package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 助手绑定 MCP 列表 VO（仅 MCP 纬度，不展示具体工具）
 * <p>
 * 用于：助手设置弹窗、手动模式 MCP 选择器。
 */
@Getter
@Setter
public class McpBindVO {

    /**
     * MCP Server ID
     */
    private Long mcpServerId;

    /**
     * MCP 服务端名称
     */
    private String mcpServerName;

    /**
     * 传输方式：STDIO / SSE / STREAMABLE_HTTP
     */
    private String transportType;

    /**
     * MCP 全局启用状态
     */
    private Boolean enabled;

    /**
     * 当前助手是否已绑定此 MCP 下的至少一个工具
     */
    private Boolean bound;
}
