/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工具源 VO（按 MCP Server 分组的工具列表）
 */
@Getter
@Setter
public class ToolSourceVO {

    /**
     * MCP Server ID
     */
    private Long mcpServerId;

    /**
     * MCP Server 名称
     */
    private String mcpServerName;

    /**
     * 传输类型
     */
    private String transportType;

    /**
     * 当前全局启用状态
     */
    private Boolean enabled;

    /**
     * 该 Server 下的工具列表
     */
    private List<ToolItemVO> tools;

    /**
     * 当前助手是否已绑定此源
     */
    private Boolean bound;
}
