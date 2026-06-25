package cc.wlizhi.eddie.common.entity;

import cc.wlizhi.eddie.common.enums.ToolType;
import lombok.Getter;
import lombok.Setter;

/**
 * 工具定义表映射实体
 */
@Setter
@Getter
public class ToolDefinitionEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 工具类型：BUILT_IN（内置）/ MCP（MCP 工具）
     */
    private ToolType toolType;

    /**
     * 工具唯一标识名，如 'fetch_markdown', 'fetch_json'
     */
    private String name;

    /**
     * 工具显示名称
     */
    private String displayName;

    /**
     * 工具功能描述
     */
    private String description;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 0=用户自定义, 1=内置（不可删除）
     */
    private Integer builtIn;

    /**
     * 关联 ai_mcp_server.id（仅 MCP 类型）
     */
    private Long mcpServerId;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
