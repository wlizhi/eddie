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
 *   event: milestone      — 关键里程碑
 *   event: round_start    — 新一轮迭代开始
 *   event: metadata       — 执行完毕元数据
 *   event: message_created— 消息已持久化
 *   event: cancelled      — 用户停止回答
 *   event: error          — 服务端错误
 */

import type {ApiResult} from '@/types/chat'
import type {AgentMessageVO, AgentStreamChatOptions} from '@/types/agent-chat'

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
        onMilestone, onRoundStart, onMessageCreated, onPlanStarted, onPlanGenerated,
        onTaskPlan, onCancelled, onComplete, onError, signal,
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
            const data = dataLines.join('\n')
            dataLines = []

            switch (currentEvent) {
                case 'thinking':
                    try {
                        // 后端使用 AgentEventPublisher 发射 JSON envelope: { msgId, stepId, data: { text } }
                        const envelope = JSON.parse(data) as {
                            msgId?: number;
                            stepId?: number;
                            data?: { text: string }
                        }
                        onThinking?.(envelope.data?.text ?? data)
                    } catch {
                        onThinking?.(data)
                    }
                    break
                case 'answer':
                    try {
                        const envelope = JSON.parse(data) as {
                            msgId?: number;
                            stepId?: number;
                            data?: { text: string }
                        }
                        onAnswer?.(envelope.data?.text ?? data)
                    } catch {
                        onAnswer?.(data)
                    }
                    break
                case 'metadata':
                    try {
                        // 后端使用 AgentEventPublisher 发射 JSON envelope: { msgId, stepId, data: { ...stats } }
                        const envelope = JSON.parse(data) as {
                            msgId?: number;
                            stepId?: number;
                            data?: Record<string, unknown>
                        }
                        onMetadata?.(JSON.stringify(envelope.data ?? {}))
                    } catch {
                        onMetadata?.(data)
                    }
                    break
                case 'tool_execution':
                    try {
                        // 后端使用 AgentEventPublisher 发射 JSON envelope: { msgId, stepId, data: { status, toolName, ... } }
                        const envelope = JSON.parse(data) as {
                            msgId?: number
                            stepId?: number
                            data?: {
                                status: string
                                toolName: string
                                arguments?: string
                                result?: string
                                error?: boolean
                            }
                        }
                        if (envelope.data) {
                            onToolExecution?.(envelope.data)
                        }
                    } catch {
                        // ignore parse error
                    }
                    break
                case 'milestone':
                    try {
                        const parsed = JSON.parse(data) as {
                            title: string
                            description?: string
                            type?: string
                            details?: Record<string, unknown>
                        }
                        onMilestone?.({
                            title: parsed.title,
                            description: parsed.description,
                            type: parsed.type as 'info' | 'success' | 'warning' | 'error' | undefined,
                            details: parsed.details,
                        })
                    } catch {
                        // ignore parse error
                    }
                    break
                case 'round_start':
                    try {
                        const parsed = JSON.parse(data) as { round: number }
                        onRoundStart?.(parsed.round)
                    } catch {
                        onRoundStart?.(0)
                    }
                    break
                case 'message_created':
                    try {
                        // 后端 emit 使用 envelope 包装：{ msgId, stepId, data: { ... } }
                        const envelope = JSON.parse(data) as {
                            msgId?: number
                            data?: { userMsgId?: number; assistantMsgId?: number }
                        }
                        onMessageCreated?.(envelope.data ?? {})
                    } catch {
                        // ignore parse error
                    }
                    break
                case 'plan_started':
                    onPlanStarted?.()
                    break
                case 'plan_generated':
                    try {
                        // 后端使用 AgentEventPublisher 发射 JSON envelope: { msgId, stepId, data: AgentTaskPlan }
                        const planEnvelope = JSON.parse(data) as {
                            msgId?: number
                            stepId?: number
                            data?: import('@/types/agent-chat').AgentTaskPlan
                        }
                        if (planEnvelope.data) {
                            onPlanGenerated?.(planEnvelope.data)
                        }
                    } catch {
                        // ignore parse error
                    }
                    break
                case 'update_task_plan':
                    try {
                        // 后端使用 AgentEventPublisher 发射 JSON envelope: { msgId, stepId, data: AgentTaskPlan }
                        const envelope = JSON.parse(data) as {
                            msgId?: number
                            stepId?: number
                            data?: import('@/types/agent-chat').AgentTaskPlan
                        }
                        if (envelope.data) {
                            onTaskPlan?.(envelope.data)
                        }
                    } catch {
                        // ignore parse error
                    }
                    break
                case 'cancelled':
                    try {
                        const parsed = JSON.parse(data) as { reason: string }
                        onCancelled?.(parsed.reason)
                    } catch {
                        onCancelled?.('unknown')
                    }
                    break
                case 'error':
                    try {
                        const parsed = JSON.parse(data) as { message: string }
                        onError?.(new Error(parsed.message))
                    } catch {
                        onError?.(new Error('请求被拒绝'))
                    }
                    break
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
