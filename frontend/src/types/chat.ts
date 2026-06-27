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
    /**
     * 思考模式：auto / low / medium / high / max / disabled
     * - auto: 不传递参数，让模型自己决定
     * - low/medium/high: 对应 reasoning_effort 参数
     * - max: 最大思考力度（DeepSeek 特有）
     * - disabled: 禁用思考
     */
    thinkingMode?: string
}

/**
 * 工具执行事件数据（对应后端 ToolExecutionEvent）
 */
export interface ToolExecutionData {
    /** start | complete */
    status: string
    /** 工具名称，如 built_in_search */
    toolName: string
    /** 工具调用参数（JSON 字符串） */
    arguments?: string
    /** 工具执行结果 */
    result?: string
    /** 是否执行出错 */
    error?: boolean
}

/**
 * SSE 事件类型
 */
export type SSESseEventType = 'thinking' | 'answer' | 'metadata' | 'tool_execution'

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
 * 运行时工具执行记录（附属于 ChatMessage）
 */
export interface ToolExecutionRecord {
    toolName: string
    arguments?: string
    result?: string
    error?: boolean
    done: boolean
}

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
    toolCalls?: ToolExecutionRecord[]
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
    /** 缓存读取的 input token 数 */
    cacheReadInputTokens?: number
    /** 缓存写入的 input token 数 */
    cacheWriteInputTokens?: number
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
