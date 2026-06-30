/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.settings.entity.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 内置工具启用/禁用状态更新请求
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>MCP 级别切换：传入 mcpServerId + enabled，批量更新该 MCP 下所有工具</li>
 *   <li>工具级别切换：传入 toolId + enabled，更新单个工具</li>
 * </ul>
 */
@Getter
@Setter
public class BuiltInStatusUpdateRequest {

    /**
     * MCP 级别切换时传入，批量更新该服务下所有工具
     */
    private Long mcpServerId;

    /**
     * 工具级别切换时传入，更新单个工具
     */
    private Long toolId;

    /**
     * 目标启用状态
     */
    private Boolean enabled;
}
