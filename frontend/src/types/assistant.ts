/**
 * 助手相关类型定义
 *
 * 对应后端：
 *   AssistantVO       → 列表展示
 *   AssistantDetailVO → 详情回显（含 modelParams）
 *   AssistantCreateRequest  → 新建请求
 *   AssistantUpdateRequest  → 更新请求（部分更新）
 *   ModelParams       → 模型参数
 */

/**
 * 系统提示词模板变量信息（对应后端 VariableInfo）
 */
export interface PromptVariableInfo {
    /** 变量名，如 "datetime" */
    key: string
    /** 模板字符串，如 "${datetime}" */
    template: string
    /** 示例值，如 "2026-06-28 19:57" */
    example: string
    /** 描述，如 "当前日期和时间" */
    description: string
}

/**
 * 模型参数（对应后端 ModelParams）
 */
export interface ModelParams {
    temperature?: number
    maxTokens?: number
    topP?: number
    frequencyPenalty?: number
    presencePenalty?: number
    topK?: number
    stop?: string[]
    /** 默认思考模式：auto / low / medium / high / max / disabled */
    thinkingMode?: string
    extensions?: Record<string, unknown>
}

/**
 * 助手偏好设置
 * 用于控制前端 UI 默认状态，存在数据库 preferences 列（JSON）
 */
export interface AssistantPreferences {
    /** 联网搜索默认开关 */
    webSearchEnabled?: boolean
    /** MCP 工具默认模式：disabled / auto / manual */
    mcpToolMode?: 'disabled' | 'auto' | 'manual'

    /** 扩展字段，支持未来新增偏好 */
    [key: string]: unknown
}

/**
 * 助手列表项 VO（对应后端 AssistantVO）
 */
export interface AssistantVO {
    id: number
    name: string
    avatar: string | null
    description: string
    systemPrompt: string
    providerId: number
    providerName: string
    modelId: string
    memoryRounds: number
    enabled: number       // 0=禁用, 1=启用
    sortOrder: number
    createdAt: string
    updatedAt: string
}

/**
 * 助手详情 VO（对应后端 AssistantDetailVO）
 */
export interface AssistantDetailVO extends Omit<AssistantVO, 'enabled'> {
    providerCode: string
    modelParams: ModelParams | null
    /** 助手偏好设置 */
    preferences: AssistantPreferences | null
    /** 注意：后端详情接口返回 boolean（true/false），列表接口返回 number（1/0） */
    enabled: number | boolean
    /** 已绑定的 MCP Server ID 列表（回显用） */
    boundMcpServerIds: number[]
}

/**
 * 新建助手请求参数（对应后端 AssistantCreateRequest）
 */
export interface AssistantCreateRequest {
    name: string
    avatar?: string
    description?: string
    systemPrompt?: string
    providerId?: number
    modelId: string
    modelParams?: ModelParams
    preferences?: AssistantPreferences
    memoryRounds?: number
    /** 启用的 MCP Server ID 列表 */
    enabledMcpServerIds?: number[]
}

/**
 * 更新助手请求参数（对应后端 AssistantUpdateRequest，部分更新）
 */
export interface AssistantUpdateRequest {
    name?: string
    avatar?: string
    description?: string
    systemPrompt?: string
    providerId?: number
    modelId?: string
    modelParams?: ModelParams
    preferences?: AssistantPreferences
    memoryRounds?: number
    enabled?: number
    sortOrder?: number
    /** 启用的 MCP Server ID 列表（传入后全量替换） */
    enabledMcpServerIds?: number[]
}
