/**
 * 聊天模型选择器 — 单个模型项
 * 对应后端 ChatModelItemVO
 */
export interface ChatModelItem {
    /** 模型 ID，如 "deepseek-v4-flash" */
    modelId: string
    /** 模型显示名，为 null 时前端 fallback 到 modelId */
    displayName: string | null
    /** 模型提供商代码，如 "deepseek" */
    providerCode: string
}

/**
 * 聊天模型选择器 — 按供应商分组
 * 对应后端 ChatModelSelectorVO
 */
export interface ChatModelSelector {
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
    /** 供应商 code，来自 ChatModelItemVO.providerCode */
    providerCode: string
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
    role: MessageRole
    content: string
    thinking?: string
    timestamp: number
    metadata?: ChatMetadata | null
}

/**
 * 回答完毕后的元数据
 */
export interface ChatMetadata {
    durationMs?: number
    inputTokens?: number
    outputTokens?: number
    totalTokens?: number

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
