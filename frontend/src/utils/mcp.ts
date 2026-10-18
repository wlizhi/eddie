/**
 * @author Eddie
 * @date 2026-10-18
 */

/**
 * MCP 工具展示名称。
 * <p>
 * 远端 MCP 工具没有友好标题，displayName 历史数据可能落入了长描述，
 * 因此 MCP 类型工具统一展示 name，内置工具优先展示 displayName。
 */
export interface McpToolLabel {
    name: string
    displayName?: string | null
    toolType?: string | null
}

export function mcpToolLabel(tool: McpToolLabel): string {
    if (tool.toolType === 'MCP') {
        return tool.name
    }
    return tool.displayName || tool.name
}
