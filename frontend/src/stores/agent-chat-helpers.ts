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

/** 将 AgentMessageVO.toolCalls（已解析的数组）映射为 ToolExecutionRecord[]，确保字段兼容 */
export function parseToolCalls(toolCalls: ToolExecutionRecord[] | null | undefined): ToolExecutionRecord[] {
    if (!toolCalls || !Array.isArray(toolCalls)) return []
    return toolCalls.map(item => ({
        toolName: item.toolName,
        arguments: item.arguments,
        result: item.result,
        error: !!item.error,
        done: true,
        seq: item.seq,
    }))
}

/**
 * 在消息的 steps（rounds）中按 stepRecordId 查找或创建步骤。
 *
 * 定位规则：
 * - stepRecordId = null/undefined → 最外层主步骤
 * - stepRecordId = DB ID → 步骤级子步骤
 *
 * 插入位置：
 * - null step 始终在最前面
 * - 非 null step 按 stepRecordId 升序排列
 */
export function findOrCreateStep(msg: ChatMessage, stepRecordId?: number | null): RoundContent {
    if (!msg.rounds) {
        msg.rounds = []
    }
    const rid = stepRecordId ?? null
    const existing = msg.rounds.find(r => (r.stepRecordId ?? null) === rid)
    if (existing) return existing

    const newStep: RoundContent = {
        stepRecordId: rid,
        thinking: '',
        toolCalls: [],
        content: '',
    }

    if (rid === null) {
        // null step 插入到最前面
        msg.rounds.unshift(newStep)
    } else {
        // 非 null step：找到第一个比它大的非 null step，在其前面插入
        const insertIdx = msg.rounds.findIndex(r => r.stepRecordId != null && r.stepRecordId > rid)
        if (insertIdx === -1) {
            msg.rounds.push(newStep)
        } else {
            msg.rounds.splice(insertIdx, 0, newStep)
        }
    }
    return newStep
}

/** 将后端 AgentMessageVO 转换为前端 ChatMessage */
export function toChatMessage(vo: {
    id: number
    role: string
    thinking: string
    content: string
    toolCalls: ToolExecutionRecord[]
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
    // 从 stepList 构建 rounds，每个 step 对应一个轮次
    if (vo.role === 'assistant' && vo.stepList && vo.stepList.length > 0) {
        msg.rounds = vo.stepList.map(step => ({
            stepRecordId: step.id,
            thinking: step.thinking || '',
            toolCalls: parseToolCalls(step.toolCalls),
            content: step.content || '',
        }))
    } else if (vo.role === 'assistant' && (thinking || toolCalls.length > 0 || content)) {
        // 兼容无 stepList 的历史消息（旧数据），回填为单轮次
        msg.rounds = [{
            stepRecordId: null,
            thinking: thinking ?? '',
            toolCalls,
            content,
        }]
    }
    return msg
}
