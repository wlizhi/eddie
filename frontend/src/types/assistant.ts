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
    memoryRounds?: number
    enabled?: number
    sortOrder?: number
    /** 启用的 MCP Server ID 列表（传入后全量替换） */
    enabledMcpServerIds?: number[]
}
