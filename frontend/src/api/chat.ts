import type {ApiResult, ChatModelSelector, ChatRequest} from '@/types/chat'

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
export async function streamChat(options: StreamChatOptions): Promise<void> {
    const {request, onThinking, onAnswer, onMetadata, onComplete, onError, signal} = options

    try {
        const response = await fetch(`${BASE_URL}/chat/send`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(request),
            signal,
        })

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`)
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
