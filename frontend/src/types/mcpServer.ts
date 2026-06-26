/**
 * 对应后端 McpServerVO
 */
export interface McpServer {
    id: number
    name: string
    transportType: 'STDIO' | 'SSE' | 'STREAMABLE_HTTP' | 'BUILT_IN'
    command: string
    args: string
    env: string
    url: string
    timeoutSeconds: number
    enabled: boolean
    builtIn: boolean
    sortOrder: number
    createdAt: string
    updatedAt: string
    tools: McpToolItem[]
}

/**
 * 对应后端 McpToolItemVO
 */
export interface McpToolItem {
    id: number
    name: string
    displayName: string
    description: string
    toolType: 'BUILT_IN' | 'MCP'
    enabled: boolean
    builtIn: boolean
    sortOrder: number
}

/**
 * 对应后端 McpServerCreateRequest
 */
export interface McpServerCreateRequest {
    name: string
    transportType: 'STDIO' | 'SSE' | 'STREAMABLE_HTTP'
    command?: string
    args?: string
    env?: string
    url?: string
    timeoutSeconds?: number
    sortOrder?: number
}

/**
 * 传输方式显示配置
 */
export const TRANSPORT_LABELS: Record<string, string> = {
    STDIO: 'STDIO',
    SSE: 'SSE',
    STREAMABLE_HTTP: 'HTTP',
    BUILT_IN: '内置',
}

export const TRANSPORT_CLASSES: Record<string, string> = {
    STDIO: 'stdio',
    SSE: 'sse',
    STREAMABLE_HTTP: 'http',
    BUILT_IN: 'built-in',
}
