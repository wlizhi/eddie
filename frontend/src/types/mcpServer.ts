/**
 * 来源类型：BUILT_IN（内置工具）/ USER（用户自定义）/ PROVIDER（第三方服务商）
 */
export type McpSourceType = 'BUILT_IN' | 'USER' | 'PROVIDER'

/**
 * 对应后端 McpServerVO
 */
export interface McpServer {
    id: number
    name: string
    description: string
    sourceType: McpSourceType
    transportType: 'STDIO' | 'SSE' | 'STREAMABLE_HTTP' | 'BUILT_IN'
    command: string
    args: string
    env: string
    url: string
    headers: string
    timeoutSeconds: number
    enabled: boolean
    sortOrder: number
    reconnectIntervalSec: number
    maxReconnectAttempts: number
    createdAt: string
    updatedAt: string
    connectionStatus: 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING'
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
    description?: string
    sourceType?: McpSourceType
    sourceConfig?: string
    transportType: 'STDIO' | 'SSE' | 'STREAMABLE_HTTP'
    command?: string
    args?: string
    env?: string
    url?: string
    headers?: string
    timeoutSeconds?: number
    sortOrder?: number
    reconnectIntervalSec?: number
    maxReconnectAttempts?: number
    enabled?: boolean
}

/**
 * 对应后端 McpServerUpdateRequest（继承 CreateRequest，增加 id）
 */
export interface McpServerUpdateRequest extends McpServerCreateRequest {
    id: number
}

/**
 * 对应后端 ToolItemVO — MCP Server 下的单个工具
 */
export interface ToolItemVO {
    id: number
    name: string
    displayName: string
    description: string
    toolType: string
    enabled: boolean
}

/**
 * 对应后端 ToolSourceVO — MCP Server + 下辖工具二层结构
 */
export interface ToolSourceVO {
    mcpServerId: number
    mcpServerName: string
    transportType: string
    enabled: boolean
    tools: ToolItemVO[]
    bound: boolean
}

/**
 * 对应后端 McpConnectResult — 连接测试结果
 */
export interface McpConnectResult {
    connected: boolean
    message: string
    tools: McpToolItem[]
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
