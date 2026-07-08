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
    stepId?: number
    step?: number
    text: string
}

export interface AnswerPayload {
    msgId?: number
    stepId?: number
    step?: number
    text: string
}

export interface ToolExecutionPayload {
    msgId?: number
    stepId?: number
    step?: number
    toolName: string
    status: string
    arguments?: string
    result?: string
    error?: boolean
}

export interface MessageCreatedPayload {
    msgId?: number
    stepId?: number
    step?: number
    userMsgId?: number
    assistantMsgId?: number
}

export interface RoundStartPayload {
    msgId?: number
    stepId?: number
    step?: number
    round: number
}

export interface CancelledPayload {
    msgId?: number
    stepId?: number
    step?: number
    reason: string
}
