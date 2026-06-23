/**
 * 聊天模型选择器 — 单个模型项
 * 对应后端 ChatModelItemVO
 */
export interface ChatModelItem {
    /** 模型 ID，如 "deepseek-v4-flash" */
    modelId: string
    /** 模型显示名，为 null 时前端 fallback 到 modelId */
    displayName: string | null
    /** 所属供应商实例 ID */
    providerId: number
    /** 模型提供商代码，如 "deepseek" */
    providerCode: string
}

/**
 * 聊天模型选择器 — 按供应商实例分组
 * 对应后端 ChatModelSelectorVO
 */
export interface ChatModelSelector {
    providerId: number
    providerCode: string
    providerName: string
    models: ChatModelItem[]
}

/**
 * 聊天请求参数
 * 对应后端 ChatRequest
 */
export interface ChatRequest {
    conversationId: string
    message: string
    /** 供应商实例 ID，来自 ChatModelItemVO.providerId */
    providerId: number
    /** 模型 ID，来自 ChatModelItemVO.modelId */
    modelId: string
}

/**
 * SSE 事件类型
 */
export type SSESseEventType = 'thinking' | 'answer' | 'metadata'

/**
 * SSE 原始事件行解析结果
 */
export interface SSEEvent {
    event: string
    data: string
}

/**
 * 消息角色
 */
export type MessageRole = 'user' | 'assistant'

/**
 * 单条聊天消息
 */
export interface ChatMessage {
    id: string
    /** 后端数据库 ID（仅从接口加载的消息有值，用于游标分页） */
    dbId?: number
    role: MessageRole
    content: string
    thinking?: string
    timestamp: number
    metadata?: ChatMetadata | null
    /** 预渲染的 Markdown 内容 HTML，避免模板中重复 parse */
    renderedContent?: string
    /** 预渲染的 thinking Markdown 内容 HTML */
    renderedThinking?: string
}

/**
 * 回答完毕后的元数据
 */
export interface ChatMetadata {
    /** 本轮对话耗时（毫秒） */
    durationMs?: number
    /** 结束时间戳（毫秒） */
    timestamp?: number
    /** prompt token 数 */
    promptTokens?: number
    /** completion token 数 */
    completionTokens?: number
    /** 总 token 数 */
    totalTokens?: number
    /** 预估费用（美元） */
    costEstimate?: number
    /** 币种代码，如 USD、CNY（为空时默认 $） */
    currency?: string

    [key: string]: unknown
}

/**
 * API 通用响应结构
 * 对应后端 ApiResult
 */
export interface ApiResult<T> {
    code: number
    message: string
    data: T
}
