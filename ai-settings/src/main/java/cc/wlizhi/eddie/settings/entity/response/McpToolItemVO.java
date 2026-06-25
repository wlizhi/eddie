package cc.wlizhi.eddie.settings.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * MCP 工具项 VO（二层结构中内层）
 */
@Getter
@Setter
public class McpToolItemVO {

    /**
     * 工具 ID
     */
    private Long id;

    /**
     * 工具唯一标识名，如 'file_read'
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
     * 工具类型：BUILT_IN / MCP
     */
    private String toolType;

    /**
     * 当前全局启用状态
     */
    private Boolean enabled;

    /**
     * 0=用户自定义, 1=内置（不可删除）
     */
    private Boolean builtIn;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
