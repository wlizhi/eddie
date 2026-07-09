/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * 会话相关类型定义
 *
 * 对应后端：
 *   SessionVO            → 会话列表展示
 *   MessageVO            → 消息列表
 *   SessionCreateRequest  → 创建会话请求
 *   TitleRenameRequest   → 重命名请求
 *   TitleGenerateRequest → AI 生成标题请求
 */

/** 会话列表项 */
export interface SessionVO {
    id: number
    /** 归属助手 ID（助手会话） */
    assistantId?: number
    /** 归属智能体 ID（智能体会话） */
    agentId?: number
    title: string
    pinned: number          // 0=普通, 1=置顶
    messageCount: number
    totalTokens?: number
    updatedAt: number
}

/** 消息记录 */
export interface MessageVO {
    id: number
    role: string           // user / assistant / system
    providerId: number | null
    modelCode: string
    modelName: string
    thinking: string
    content: string
    promptTokens: number
    completionTokens: number
    totalTokens: number
    priceEstimate: number
    cacheReadInputTokens: number
    cacheWriteInputTokens: number
    currency: string
    durationMs?: number
    toolCalls?: ToolExecutionEventItem[]
    createdAt: number
}

/**
 * 历史消息中的工具调用记录项
 * 对应后端 ToolExecutionEvent，与 ToolExecutionRecord 的区别是使用 status 而非 done
 */
export interface ToolExecutionEventItem {
    status: 'start' | 'complete' | 'rejected'
    toolName: string
    arguments?: string
    result?: string
    error: boolean
}

/** 创建会话请求 */
export interface SessionCreateRequest {
    assistantId: number
}

/** 手动重命名请求 */
export interface TitleRenameRequest {
    title: string
}

/** AI 生成标题请求 */
export interface TitleGenerateRequest {
    providerId: number
    modelCode: string
}

/** 通用分页结果 */
export interface PageResult<T> {
    page: number
    size: number
    total: number
    records: T[]
}
