/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 工具项 VO（工具源列表中的单个工具）
 */
@Getter
@Setter
public class ToolItemVO {

    /**
     * 工具 ID
     */
    private Long id;

    /**
     * 工具唯一标识名，如 'built_in_search'
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
     * 当前全局启用状态（兼容旧版，由 enabledStatus 推导）
     */
    private Boolean enabled;

    /**
     * 工具启用状态码：0=禁用, 1=启用, 2=待审批
     */
    private Integer enabledStatus;
}
