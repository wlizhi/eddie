/**
 * @author Eddie
 * @date 2026-07-04
 */

import type {ToolExecutionRecord} from '@/types/chat'

/**
 * 智能体聊天相关类型定义
 *
 * 对应后端：
 *   AgentChatRequest         → 发送聊天请求
 *   AgentSSEEvent            → SSE 流式事件
 *   AgentMessageVO           → 消息记录
 *   MilestoneEvent           → 里程碑事件数据
 */

/**
 * 智能体聊天请求参数
 * 对应后端 AgentChatRequest
 */
export interface AgentChatRequest {
    /** 智能体 ID */
    agentId: number
    /** 会话 ID（续聊时传入，新会话不传） */
    conversationId?: number
    /** 用户消息内容 */
    message: string
    /** 用户额外指令（可选） */
    extraInstruction?: string
    /** 思考模式：auto / low / medium / high / max / disabled */
    thinkingMode?: string
    /** 工具选择模式（auto / manual / none） */
    toolSelectionMode?: string
    /** 手动模式下指定的工具名称列表 */
    toolNames?: string[]

    // ==================== 用户临时覆盖参数 ====================

    /** 临时覆盖的主模型服务商 ID（优先级高于 Agent 配置） */
    providerId?: number
    /** 临时覆盖的主模型 ID（优先级高于 Agent 配置） */
    modelId?: string
    /** 联网搜索：true=启用 */
    webSearchEnabled?: boolean
}

/**
 * 待办事项 — 计划清单中的单个步骤
 */
export interface AgentTodoItem {
    /** 步骤序号（从 1 开始） */
    id: number
    /** 步骤标题（简短精炼，对应后端 AgentTaskStep.title） */
    title: string
    /** 步骤描述（对应后端 AgentTaskStep.description） */
    description: string
    /** pending / processing / completed / failed */
    status: string
}

/**
 * 任务计划 — 对应后端 AgentTaskPlan
 */
export interface AgentTaskPlan {
    /** 任务标题 */
    title: string
    /** 任务概述 */
    summary: string
    /** planned / executing / completed / failed */
    status: string
    /** 最终汇总文本 */
    result: string
    /** 步骤列表（对应后端 AgentTaskPlan.steps） */
    steps: AgentTodoItem[]
}

/**
 * 智能体 SSE 事件类型
 */
export type AgentSSEEventType =
    | 'thinking'
    | 'answer'
    | 'tool_execution'
    | 'round_start'
    | 'metadata'
    | 'message_created'
    | 'cancelled'
    | 'error'
    | 'plan_started'
    | 'plan_generated'
    | 'update_task_plan'

/**
 * 消息记录（从后端加载的历史消息）
 * 对应后端 AgentMsgEntity
 */
export interface AgentMessageVO {
    id: number
    sessionId: number
    agentId: number
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
    durationMs: number
    /** JSON 字符串：工具调用数组 */
    toolCalls: string
    msgStatus: string      // COMPLETED / STOPPED / ERROR
    createdAt: number
    /** 规划清单（已解析的对象，前端直接使用，仅 PLAN 模式的 assistant 消息有值） */
    taskPlan: AgentTaskPlan | null
    /** 前端展示步骤列表（msg_type=0，按 step ASC 排序） */
    stepList: AgentMsgStepVO[] | null
}

/**
 * 消息步骤明细 — 对应后端 AgentMsgStepEntity（前端展示）
 */
export interface AgentMsgStepVO {
    id: number
    msgId: number
    msgType: number
    msgDataType: number
    step: number
    stepDesc: string
    prompt: string
    thinking: string
    content: string
    toolCalls: string
    createdAt: number
}

/**
 * 单轮次内容 — 独立的一组 thinking + toolCalls + content
 */
export interface RoundContent {
    thinking: string
    toolCalls: ToolExecutionRecord[]
    content: string
}

/**
 * SSE 流式聊天选项
 */
export interface AgentStreamChatOptions {
    /** 请求参数 */
    request: AgentChatRequest
    /** 每次收到 thinking 内容时的回调（step=null 表示 CHAT 轮次，step=N 表示 EXECUTE 步骤） */
    onThinking?: (chunk: string, step?: number | null) => void
    /** 每次收到 answer 内容时的回调（step=null 表示 CHAT 轮次，step=N 表示 EXECUTE 步骤） */
    onAnswer?: (chunk: string, step?: number | null) => void
    /** 收到完整 metadata JSON 时的回调 */
    onMetadata?: (json: string) => void
    /** 收到工具执行事件时的回调（step=null 表示 CHAT 轮次，step=N 表示 EXECUTE 步骤） */
    onToolExecution?: (data: {
        status: string
        toolName: string
        arguments?: string
        result?: string
        error?: boolean
    }, step?: number | null) => void
    /** 收到新一轮迭代开始事件时的回调 */
    onRoundStart?: (round: number) => void
    /** 收到 message_created 事件时的回调 */
    onMessageCreated?: (data: { userMsgId?: number; assistantMsgId?: number }) => void
    /** 收到 plan_started 事件时的回调（模型开始生成任务清单） */
    onPlanStarted?: () => void
    /** 收到 plan_generated 事件时的回调（任务清单首次生成完毕） */
    onPlanGenerated?: (plan: AgentTaskPlan) => void
    /** 收到 update_task_plan 事件时的回调（后续清单内容更新） */
    onTaskPlan?: (plan: AgentTaskPlan) => void
    /** 收到 execute_complete 事件时的回调（步骤执行完成） */
    onExecuteComplete?: (step: number) => void
    /** 收到 cancelled 事件时的回调 */
    onCancelled?: (reason: string) => void
    /** 流结束时的回调 */
    onComplete?: () => void
    /** 出错时的回调 */
    onError?: (error: Error) => void
    /** AbortSignal，用于中断请求 */
    signal?: AbortSignal
}
