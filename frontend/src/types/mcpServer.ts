/**
 * @author Eddie
 * @date 2026-06-26
 */

/**
 * 来源类型：BUILT_IN（内置工具）/ USER（用户自定义）/ PROVIDER（第三方服务商）
 */
export type McpSourceType = 'BUILT_IN' | 'USER' | 'PROVIDER'

/**
 * 配置字段描述（对应后端 ConfigFieldDescriptor）
 */
export interface ConfigFieldDescriptor {
    name: string
    type: 'string' | 'number' | 'boolean' | 'select' | 'textarea'
    label: string
    description: string
    defaultValue: any
    placeholder?: string
    required?: boolean
    min?: number
    max?: number
    options?: { value: string; label: string }[]
    /** 依赖的字段名：仅当依赖字段的值等于 dependsOnValue 时才显示该字段 */
    dependsOn?: string
    /** 依赖字段的目标值 */
    dependsOnValue?: any
}

/**
 * 内置工具配置描述 Schema（对应后端 ConfigSchema）
 */
export interface ConfigSchema {
    /** 关联的工具名（@Tool name），MCP 内唯一 */
    toolName: string
    title: string
    description: string
    fields: ConfigFieldDescriptor[]
}

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
    createdAt: number
    updatedAt: number
    connectionStatus: 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING'
    tools: McpToolItem[]
    /** 来源配置 JSON（仅 BUILT_IN 类型有值） */
    sourceConfig?: string
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
    /** 启用状态码：0=禁用, 1=启用, 2=待审批 */
    enabledStatus: 0 | 1 | 2
    builtIn: boolean
    sortOrder: number
    /** 配置描述 Schema（仅 BUILT_IN 类型有值） */
    configSchema?: ConfigSchema
}

/**
 * 对应后端 McpServerCreateRequest
 */
export interface McpServerCreateRequest {
    name: string
    description?: string
    sourceType?: McpSourceType
    sourceConfig?: string
    transportType: 'STDIO' | 'SSE' | 'STREAMABLE_HTTP' | 'BUILT_IN'
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
    /** 当前全局启用状态（兼容旧版，由 enabledStatus 推导） */
    enabled: boolean
    /** 工具启用状态码：0=禁用, 1=启用, 2=待审批 */
    enabledStatus?: 0 | 1 | 2
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
