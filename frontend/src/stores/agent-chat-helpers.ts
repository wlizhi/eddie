/**
 * @author Eddie
 * @date 2026-07-08
 */

/**
 * agent-chat 纯函数工具集
 *
 * 从 agent-chat.ts 提取，供 store 内部使用。
 * 所有函数均为纯函数（除 debounceRender 外），输入→输出确定。
 */
import type {ChatMessage, ToolExecutionRecord} from '@/types/chat'
import type {RoundContent} from '@/types/agent-chat'
import type {ToolExecutionEventItem} from '@/types/session'
import {renderMd} from '@/utils/markdown'

/**
 * 生成唯一 ID（兼容 Safari 15.4 以下不支持 crypto.randomUUID）
 */
export function generateId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID()
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
    })
}

/** 渲染防抖 ID（requestAnimationFrame），避免每字触发全量 Markdown 解析 */
let renderRafId = 0

/**
 * 防抖渲染 Markdown：将流式内容防抖到下一帧渲染，
 * 避免每个 chunk 都触发 renderMd 全量解析 + v-html DOM 替换。
 */
export function debounceRender(msg: ChatMessage): void {
    cancelAnimationFrame(renderRafId)
    renderRafId = requestAnimationFrame(() => {
        msg.renderedContent = renderMd(msg.content || '')
    })
}

/** 取消正在等待的渲染（metadata 到达时强制最终渲染） */
export function cancelPendingRender(): void {
    cancelAnimationFrame(renderRafId)
}

/** 立即渲染最终内容 */
export function renderFinalContent(msg: ChatMessage): void {
    cancelAnimationFrame(renderRafId)
    msg.renderedContent = renderMd(msg.content || '')
}

/** 将 AgentMessageVO.toolCalls（JSON 字符串）解析为 ToolExecutionRecord[] */
export function parseToolCalls(toolCallsJson: string | null | undefined): ToolExecutionRecord[] {
    if (!toolCallsJson) return []
    try {
        const parsed = JSON.parse(toolCallsJson) as ToolExecutionEventItem[]
        return parsed.map(item => ({
            toolName: item.toolName,
            arguments: item.arguments,
            result: item.result,
            error: item.status === 'rejected' ? false : !!item.error,
            done: item.status === 'complete' || item.status === 'rejected',
            rejected: item.status === 'rejected',
        }))
    } catch {
        return []
    }
}

/** 确保消息的 rounds 数组存在并扩展到指定索引，返回 rounds 引用 */
export function ensureRounds(msg: ChatMessage, count: number): RoundContent[] {
    if (!msg.rounds) {
        msg.rounds = []
    }
    while (msg.rounds.length <= count) {
        msg.rounds.push({thinking: '', toolCalls: [], content: ''})
    }
    return msg.rounds
}

/** 将后端 AgentMessageVO 转换为前端 ChatMessage */
export function toChatMessage(vo: {
    id: number
    role: string
    thinking: string
    content: string
    toolCalls: string
    modelName: string
    durationMs: number
    promptTokens: number
    completionTokens: number
    totalTokens: number
    priceEstimate: number
    currency: string
    cacheReadInputTokens: number
    cacheWriteInputTokens: number
    createdAt: number
    taskPlan: import('@/types/agent-chat').AgentTaskPlan | null
    stepList: import('@/types/agent-chat').AgentMsgStepVO[] | null
}): ChatMessage {
    const content = vo.content || ''
    const thinking = vo.thinking || undefined
    const toolCalls = parseToolCalls(vo.toolCalls)
    const msg: ChatMessage = {
        id: generateId(),
        dbId: vo.id,
        role: vo.role as 'user' | 'assistant',
        content,
        renderedContent: content ? renderMd(content) : '',
        thinking,
        toolCalls,
        timestamp: vo.createdAt,
        modelName: vo.modelName || undefined,
        taskPlan: vo.taskPlan ?? undefined,
        stepList: vo.stepList ?? undefined,
        metadata: {
            timestamp: vo.createdAt,
            ...(vo.durationMs != null ? {durationMs: vo.durationMs} : {}),
            ...(vo.promptTokens != null ? {promptTokens: vo.promptTokens} : {}),
            ...(vo.completionTokens != null ? {completionTokens: vo.completionTokens} : {}),
            ...(vo.totalTokens != null ? {totalTokens: vo.totalTokens} : {}),
            ...(vo.cacheReadInputTokens != null ? {cacheReadInputTokens: vo.cacheReadInputTokens} : {}),
            ...(vo.cacheWriteInputTokens != null ? {cacheWriteInputTokens: vo.cacheWriteInputTokens} : {}),
            ...(vo.priceEstimate != null ? {costEstimate: vo.priceEstimate} : {}),
            ...(vo.currency ? {currency: vo.currency} : {}),
        },
    }
    // 历史消息：从 stepList 构建 rounds，每个 step 对应一个轮次
    if (vo.role === 'assistant' && vo.stepList && vo.stepList.length > 0) {
        msg.rounds = vo.stepList.map(step => ({
            thinking: step.thinking || '',
            toolCalls: parseToolCalls(step.toolCalls),
            content: step.content || '',
        }))
    } else if (vo.role === 'assistant' && (thinking || toolCalls.length > 0 || content)) {
        // 兼容无 stepList 的历史消息（旧数据），回填为单轮次
        msg.rounds = [{
            thinking: thinking ?? '',
            toolCalls,
            content,
        }]
    }
    return msg
}
