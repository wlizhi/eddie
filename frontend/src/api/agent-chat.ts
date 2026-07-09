/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体聊天 API
 *
 * 对应后端 AgentChatController：
 *   POST /api/agent/chat  → SSE 流式聊天
 *   POST /api/agent/stop  → 停止执行
 *
 * SSE 事件类型：
 *   event: thinking       — 模型思考内容
 *   event: answer         — 模型回答内容
 *   event: tool_execution — 工具执行状态
 *   event: plan_started   — 规划开始（任务清单生成中）
 *   event: plan_generated — 规划生成成功（任务清单首次生成完毕）
 *   event: update_task_plan— 更新任务清单（全量任务清单内容）
 *   event: round_start    — 新一轮迭代开始
 *   event: metadata       — 执行完毕元数据
 *   event: message_created— 消息已持久化
 *   event: execute_complete— 步骤执行完成
 *   event: cancelled      — 用户停止回答
 *   event: error          — 服务端错误
 */

import type {ApiResult} from '@/types/chat'
import type {AgentMessageVO, AgentStreamChatOptions} from '@/types/agent-chat'
import type {
    ApiResult as AgentApiResult,
    ThinkingPayload,
    AnswerPayload,
    ToolExecutionPayload,
    MessageCreatedPayload,
    RoundStartPayload,
    CancelledPayload,
} from '@/types/agent-event'

const BASE = '/api/agent'

/**
 * 发送智能体聊天消息，通过 SSE 流式接收响应
 *
 * 与普通聊天不同，Agent 消息由后端智能体引擎接管执行流程，
 * 支持多轮迭代、工具调用、任务规划等复杂编排。
 */
export async function streamAgentChat(options: AgentStreamChatOptions): Promise<void> {
    const {
        request, onThinking, onAnswer, onMetadata, onToolExecution,
        onRoundStart, onMessageCreated, onPlanStarted, onPlanGenerated,
        onTaskPlan, onExecuteComplete, onCancelled, onComplete, onError, signal,
    } = options

    try {
        const response = await fetch(`${BASE}/chat`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(request),
            signal,
        })

        if (!response.ok) {
            let message = `HTTP ${response.status}: ${response.statusText}`
            try {
                const errBody = await response.json()
                if (errBody.message) message = errBody.message
            } catch {
                // ignore parse error
            }
            throw new Error(message)
        }

        // 兜底：非 SSE 响应（如后端返回 JSON 错误但 HTTP 状态码为 200）
        if (!response.headers.get('content-type')?.includes('text/event-stream')) {
            try {
                const errBody = await response.json()
                if (errBody.code && errBody.code !== 200) {
                    throw new Error(errBody.message || `请求失败 (${errBody.code})`)
                }
            } catch (e) {
                if ((e as Error).name !== 'AbortError') {
                    throw e
                }
            }
        }

        const reader = response.body?.getReader()
        if (!reader) {
            throw new Error('Response body is not readable')
        }

        const decoder = new TextDecoder()
        let buffer = ''
        let currentEvent = ''
        /** 当前事件已累积的 data 行（用 \n 分隔） */
        let dataLines: string[] = []

        function flushEvent() {
            if (!currentEvent || dataLines.length === 0) return
            const raw = dataLines.join('\n')
            dataLines = []

            // 统一解析 ApiResult 信封
            let envelope: AgentApiResult
            try {
                envelope = JSON.parse(raw) as AgentApiResult
            } catch {
                // JSON 解析失败，降级原始字符串
                if (currentEvent === 'error') {
                    onError?.(new Error(raw))
                }
                return
            }

            // 统一错误判断（与 REST API 同一套语义：code === 200 为成功）
            if (envelope.code !== 200) {
                const errMsg = envelope.message || '请求被拒绝'
                const err = new Error(errMsg)
                ;(err as unknown as Record<string, unknown>).detail = envelope.detail
                onError?.(err)
                return
            }

            // 按事件类型分发，data 为类型化的 Payload
            switch (currentEvent) {
                case 'thinking': {
                    const payload = envelope.data as ThinkingPayload
                    onThinking?.(payload?.text ?? raw, payload?.step)
                    break
                }
                case 'answer': {
                    const payload = envelope.data as AnswerPayload
                    onAnswer?.(payload?.text ?? raw, payload?.step)
                    break
                }
                case 'tool_execution': {
                    const payload = envelope.data as ToolExecutionPayload
                    if (payload) {
                        onToolExecution?.(payload, payload.step)
                    }
                    break
                }
                case 'round_start': {
                    const payload = envelope.data as RoundStartPayload
                    onRoundStart?.(payload?.round ?? 0)
                    break
                }
                case 'message_created': {
                    const payload = envelope.data as MessageCreatedPayload
                    onMessageCreated?.(payload ?? {})
                    break
                }
                case 'metadata': {
                    onMetadata?.(JSON.stringify(envelope.data ?? {}))
                    break
                }
                case 'cancelled': {
                    const payload = envelope.data as CancelledPayload
                    onCancelled?.(payload?.reason ?? 'unknown')
                    break
                }
                case 'error': {
                    const errMsg = envelope.message || '请求被拒绝'
                    const err = new Error(errMsg)
                    ;(err as unknown as Record<string, unknown>).detail = envelope.detail
                    onError?.(err)
                    break
                }
                case 'plan_started':
                    onPlanStarted?.()
                    break
                case 'plan_generated':
                    onPlanGenerated?.(envelope.data as import('@/types/agent-chat').AgentTaskPlan)
                    break
                case 'update_task_plan':
                    onTaskPlan?.(envelope.data as import('@/types/agent-chat').AgentTaskPlan)
                    break
                case 'execute_complete':
                    onExecuteComplete?.((envelope.data as { step?: number })?.step ?? 0)
                    break
                case 'round_end':
                case 'task_finish':
                    // 后端定义的事件，前端暂不处理，静默接受
                    break
                default:
                    console.warn('未知 SSE 事件类型:', currentEvent)
            }
        }

        while (true) {
            const {done, value} = await reader.read()
            if (done) {
                flushEvent()
                break
            }

            buffer += decoder.decode(value, {stream: true})

            // 按行解析 SSE
            const lines = buffer.split('\n')
            // 保留最后可能不完整的行
            buffer = lines.pop() ?? ''

            for (const line of lines) {
                if (line.trim() === '') {
                    // 空行 = 事件结束，触发回调并重置
                    flushEvent()
                    currentEvent = ''
                    continue
                }

                const parsed = parseSSELine(line)
                if (!parsed) continue

                if (parsed.event) {
                    // 新的事件类型，先 flush 上一个事件
                    flushEvent()
                    currentEvent = parsed.event
                } else if (currentEvent) {
                    // data 行，累积（允许空字符串）
                    if (parsed.data !== '[DONE]') {
                        dataLines.push(parsed.data)
                    }
                }
            }
        }

        onComplete?.()
    } catch (error) {
        if ((error as Error).name === 'AbortError') {
            // 用户主动中断，不算错误
            onComplete?.()
            return
        }
        onError?.(error as Error)
    }
}

/**
 * 解析 SSE 单行文本
 */
/**
 * 工具审批接口
 * <p>
 * 用户在前端点击"批准/拒绝"按钮时调用，
 * 通知后端 {@code ApprovalInterceptor} 继续执行或中断。
 */
export async function approveTool(msgId: number, toolName: string, approved: boolean, stepId?: number | null, seq?: number): Promise<void> {
    const res = await fetch('/api/agent/tools/approve', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            ownerType: 'agent',
            msgId,
            stepId: stepId ?? null,
            toolName,
            approved,
            seq: seq ?? 0,
        }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '审批请求失败')
}

function parseSSELine(line: string): { event: string; data: string } | null {
    if (line.startsWith('event:')) {
        return {event: line.slice(6).trim(), data: ''}
    }
    if (line.startsWith('data:')) {
        return {event: '', data: line.slice(5).replace(/\r$/, '')}
    }
    return null
}

/**
 * 停止智能体执行
 * POST /api/agent/stop
 *
 * @param messageId 消息 ID（来自 SSE message_created 事件的 assistantMsgId）
 */
export async function stopAgentChat(messageId: number): Promise<void> {
    const params = new URLSearchParams()
    params.set('messageId', String(messageId))
    const res = await fetch(`${BASE}/stop?${params}`, {method: 'POST'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '停止智能体执行失败')
}

/**
 * 加载智能体会话的历史消息（游标分页，倒序）
 * GET /api/agent/session/{sessionId}/messages
 *
 * @param sessionId 会话 ID
 * @param beforeId 游标：返回比此 ID 更早的消息（可选）
 * @param limit 每页数量（默认 20）
 */
export async function fetchAgentMessages(
    sessionId: number,
    beforeId?: number,
    limit: number = 20,
): Promise<AgentMessageVO[]> {
    const params = new URLSearchParams()
    params.set('limit', String(limit))
    if (beforeId != null) {
        params.set('beforeId', String(beforeId))
    }
    const res = await fetch(`${BASE}/session/${sessionId}/messages?${params}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AgentMessageVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取消息列表失败')
    return json.data
}
