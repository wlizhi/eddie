/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.settings.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 启用/禁用 MCP 或工具的状态更新请求
 * <p>
 * 支持级联联动：工具全部禁用 → MCP 自动禁用；任意工具启用 → MCP 自动启用。
 */
@Getter
@Setter
public class McpStatusUpdateRequest {

    /**
     * MCP Server ID
     */
    @NotNull(message = "MCP Server ID 不能为空")
    private Long mcpServerId;

    /**
     * MCP 级启用状态（可选，不传则不修改 MCP 本身）
     */
    private Boolean mcpEnabled;

    /**
     * 工具级启用状态列表（可选）
     */
    private List<ToolStatusItem> tools;

    /**
     * 工具状态项
     */
    @Getter
    @Setter
    public static class ToolStatusItem {

        /**
         * 工具 ID
         */
        @NotNull(message = "工具 ID 不能为空")
        private Long id;

        /**
         * 启用状态
         */
        @NotNull(message = "工具启用状态不能为空")
        private Boolean enabled;
    }
}
