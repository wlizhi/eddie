/**
 * @author Eddie
 * @date 2026-06-20
 */

import type {ApiResult, ChatModelSelector, ChatRequest, ToolExecutionData} from '@/types/chat'

const BASE_URL = '/api'

/**
 * SSE 流式聊天选项
 */
export interface StreamChatOptions {
    /** 请求参数 */
    request: ChatRequest
    /** 每次收到 thinking 内容时的回调 */
    onThinking?: (chunk: string) => void
    /** 每次收到 answer 内容时的回调 */
    onAnswer?: (chunk: string) => void
    /** 收到完整 metadata JSON 时的回调 */
    onMetadata?: (json: string) => void
    /** 收到工具执行事件时的回调（通用展示，不做工具名映射） */
    onToolExecution?: (data: ToolExecutionData) => void
    /** 收到 message_created 事件时的回调（包含 userMsgId、assistantMsgId） */
    onMessageCreated?: (data: { userMsgId: number; assistantMsgId: number }) => void
    /** 收到 cancelled 事件时的回调（用户停止回答） */
    onCancelled?: (reason: string) => void
    /** 流结束时的回调 */
    onComplete?: () => void
    /** 出错时的回调 */
    onError?: (error: Error) => void
    /** AbortSignal，用于中断请求 */
    signal?: AbortSignal
}

/**
 * 解析 SSE 单行文本，返回 { event, data }
 */
function parseSSELine(line: string): { event: string; data: string } | null {
    if (line.startsWith('event:')) {
        return {event: line.slice(6).trim(), data: ''}
    }
    if (line.startsWith('data:')) {
        // 不 trim：保留空格等有效空白字符，只去掉 \r（CRLF 兼容）
        return {event: '', data: line.slice(5).replace(/\r$/, '')}
    }
    return null
}

/**
 * 发送聊天消息，通过 SSE 流式接收响应（支持中断）
 *
 * 后端返回 SSE 格式：
 *   event: thinking
 *   data: <思考内容片段>
 *
 *   event: answer
 *   data: <回答内容片段>
 *
 *   event: metadata
 *   data: {"durationMs":123,"inputTokens":100,"outputTokens":200}
 *
 * 注意：当一个事件有多行 data 时，需要用 \n 拼接，
 * 否则 Markdown 的换行会丢失导致 marked 无法正确解析。
 */
/**
 * 停止回答
 * POST /api/chat/stop
 */
export async function stopChat(userMessageId: number, mode: 'graceful' | 'forced'): Promise<void> {
    await fetch(`${BASE_URL}/chat/stop?userMessageId=${userMessageId}&mode=${mode}`, {method: 'POST'})
}

export async function streamChat(options: StreamChatOptions): Promise<void> {
    const {request, onThinking, onAnswer, onMetadata, onToolExecution, onMessageCreated, onCancelled, onComplete, onError, signal} = options

    try {
        const response = await fetch(`${BASE_URL}/chat/send`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(request),
            signal,
        })

        if (!response.ok) {
            let message = `HTTP ${response.status}: ${response.statusText}`
            try {
                const errBody = await response.json()
                if (errBody.message) {
                    message = errBody.message
                }
            } catch {
                // ignore parse error
            }
            throw new Error(message)
        }

        // 兜底：非 SSE 响应（如后端返回 JSON 错误但 HTTP 状态码为 200），直接抛异常
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
            let data = dataLines.join('\n')
            dataLines = []
            // 安全兜底：空 data 表示 \n 被 SSE 协议消耗，恢复之
            if (data === '' && (currentEvent === 'answer' || currentEvent === 'thinking')) {
                data = '\n'
            }

            if (currentEvent === 'thinking') {
                onThinking?.(data)
            } else if (currentEvent === 'answer') {
                onAnswer?.(data)
            } else if (currentEvent === 'metadata') {
                onMetadata?.(data)
            } else if (currentEvent === 'tool_execution') {
                try {
                    // 新格式：ApiResult 信封包裹 ToolExecutionData
                    const envelope = JSON.parse(data) as ApiResult<ToolExecutionData>
                    if (envelope.data) {
                        onToolExecution?.(envelope.data)
                    }
                } catch {
                    // ignore parse error
                }
            } else if (currentEvent === 'message_created') {
                try {
                    const parsed = JSON.parse(data) as { userMsgId: number; assistantMsgId: number }
                    onMessageCreated?.(parsed)
                } catch {
                    // ignore parse error
                }
            } else if (currentEvent === 'cancelled') {
                try {
                    const parsed = JSON.parse(data) as { reason: string }
                    onCancelled?.(parsed.reason)
                } catch {
                    onCancelled?.('unknown')
                }
            } else if (currentEvent === 'error') {
                try {
                    const parsed = JSON.parse(data) as { message: string }
                    onError?.(new Error(parsed.message))
                } catch {
                    onError?.(new Error('请求被拒绝'))
                }
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
                    // data 行，累积（允许空字符串 — 对应 \n token）
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
 * 获取模型选择器列表（按供应商分组）
 * GET /api/chat-model/list
 */
export async function fetchModelList(): Promise<ChatModelSelector[]> {
    const res = await fetch(`${BASE_URL}/chat-model/list`)
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    }
    const json: ApiResult<ChatModelSelector[]> = await res.json()
    if (json.code !== 200) {
        throw new Error(json.message || '获取模型列表失败')
    }
    return json.data
}

/**
 * 工具审批接口
 * <p>
 * 对应后端：ChatToolApprovalController.approveTool
 * POST /api/chat/tools/approve
 */
export async function approveTool(
    msgId: number,
    approved: boolean,
    seq?: number,
): Promise<void> {
    const res = await fetch(`/api/chat/tools/approve`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            msgId,
            seq: seq ?? 0,
            approved,
        }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '审批请求失败')
}
