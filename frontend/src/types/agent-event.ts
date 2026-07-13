/**
 * @author Eddie
 * @date 2026-07-07
 *
 * Agent 事件 Payload 类型定义。
 *
 * 所有 SSE 事件统一使用 ApiResult<T> 作为数据容器：
 *   { code: 200, message: "ok", detail: null, data: Payload }
 *
 * code !== 200 表示异常，与后端 REST API 同一套语义。
 */

/**
 * 通用 API 响应 — 对应后端 ApiResult<T>
 */
export interface ApiResult<T = unknown> {
    code: number
    message: string
    detail?: string
    data: T
}

// ==================== 事件 Payload ====================

export interface ThinkingPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    text: string
}

export interface AnswerPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    text: string
}

export interface ToolExecutionPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    toolName: string
    status: string
    arguments?: string
    result?: string
    error?: boolean
    /** 工具调用序号（后端 Java int 原语，始终有值） */
    seq?: number
}

export interface MessageCreatedPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    userMsgId?: number
    assistantMsgId?: number
}

export interface RoundStartPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    round: number
}

export interface CancelledPayload {
    msgId?: number
    stepRecordId?: number
    stepNumber?: number
    reason: string
}
