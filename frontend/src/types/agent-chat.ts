/**
 * @author Eddie
 * @date 2026-07-04
 */

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
    question: string
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
 * 智能体 SSE 事件类型
 */
export type AgentSSEEventType =
    | 'thinking'
    | 'answer'
    | 'tool_execution'
    | 'milestone'
    | 'round_start'
    | 'metadata'
    | 'message_created'
    | 'cancelled'
    | 'error'

/**
 * 里程碑事件数据
 * 对应后端 milestone SSE 事件的 data JSON
 */
export interface MilestoneEvent {
    /** 里程碑标题 */
    title: string
    /** 里程碑描述 */
    description?: string
    /** 里程碑类型，如 info / success / warning / error */
    type?: 'info' | 'success' | 'warning' | 'error'
    /** 关联的工具调用相关信息 */
    details?: Record<string, unknown>
}

/**
 * 消息记录（从后端加载的历史消息）
 * 对应后端 AgentMsgEntity
 */
export interface AgentMessageVO {
    id: number
    sessionId: number
    agentId: number
    taskId: number | null
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
}

/**
 * SSE 流式聊天选项
 */
export interface AgentStreamChatOptions {
    /** 请求参数 */
    request: AgentChatRequest
    /** 每次收到 thinking 内容时的回调 */
    onThinking?: (chunk: string) => void
    /** 每次收到 answer 内容时的回调 */
    onAnswer?: (chunk: string) => void
    /** 收到完整 metadata JSON 时的回调 */
    onMetadata?: (json: string) => void
    /** 收到工具执行事件时的回调 */
    onToolExecution?: (data: {
        status: string
        toolName: string
        arguments?: string
        result?: string
        error?: boolean
    }) => void
    /** 收到里程碑事件时的回调 */
    onMilestone?: (event: MilestoneEvent) => void
    /** 收到新一轮迭代开始事件时的回调 */
    onRoundStart?: (round: number) => void
    /** 收到 message_created 事件时的回调 */
    onMessageCreated?: (data: { userMsgId?: number; assistantMsgId?: number }) => void
    /** 收到 cancelled 事件时的回调 */
    onCancelled?: (reason: string) => void
    /** 流结束时的回调 */
    onComplete?: () => void
    /** 出错时的回调 */
    onError?: (error: Error) => void
    /** AbortSignal，用于中断请求 */
    signal?: AbortSignal
}
