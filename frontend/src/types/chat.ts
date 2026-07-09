/**
 * @author Eddie
 * @date 2026-06-20
 */

/**
 * 聊天模型选择器 — 单个模型项
 * 对应后端 ChatModelItemVO
 */
export interface ChatModelItem {
    /** 模型 ID，如 "deepseek-v4-flash" */
    modelId: string
    /** 模型显示名，为 null 时前端 fallback 到 modelId */
    displayName: string | null
    /** 所属供应商实例 ID */
    providerId: number
    /** 模型提供商代码，如 "deepseek" */
    providerCode: string
}

/**
 * 聊天模型选择器 — 按供应商实例分组
 * 对应后端 ChatModelSelectorVO
 */
export interface ChatModelSelector {
    providerId: number
    providerCode: string
    providerName: string
    models: ChatModelItem[]
}

/**
 * 聊天请求参数
 * 对应后端 ChatRequest
 */
export interface ChatRequest {
    conversationId: string
    message: string
    /** 供应商实例 ID，来自 ChatModelItemVO.providerId */
    providerId: number
    /** 模型 ID，来自 ChatModelItemVO.modelId */
    modelId: string
    /**
     * 思考模式：auto / low / medium / high / max / disabled
     * - auto: 不传递参数，让模型自己决定
     * - low/medium/high: 对应 reasoning_effort 参数
     * - max: 最大思考力度（DeepSeek 特有）
     * - disabled: 禁用思考
     */
    thinkingMode?: string

    /**
     * 工具选择模式：auto / manual / none
     * - auto: AI 自主选择工具
     * - manual: 手动指定工具
     * - none: 禁用工具
     */
    toolSelectionMode?: string

    /**
     * 手动模式下指定的工具名称列表（toolSelectionMode=manual 时有效）
     * 由前端从选中的 MCP Server 内层展开为扁平的工具名列表
     */
    toolNames?: string[]
}

/**
 * 工具执行事件数据（对应后端 ToolExecutionEvent）
 */
export interface ToolExecutionData {
    /** start | complete | pending_approval */
    status: string
    /** 工具名称，如 built_in_search */
    toolName: string
    /** 工具调用参数（JSON 字符串） */
    arguments?: string
    /** 工具执行结果 */
    result?: string
    /** 是否执行出错 */
    error?: boolean
    /** 消息 ID（用于审批场景） */
    msgId?: number
    /** 步骤 ID（智能体场景） */
    stepId?: number
    /** 工具调用序号（审批 key 唯一标识） */
    seq?: number
}

/**
 * SSE 事件类型
 */
export type SSESseEventType = 'thinking' | 'answer' | 'metadata' | 'tool_execution'

/**
 * SSE 原始事件行解析结果
 */
export interface SSEEvent {
    event: string
    data: string
}

/**
 * 消息角色
 */
export type MessageRole = 'user' | 'assistant' | 'agent'

/**
 * 运行时工具执行记录（附属于 ChatMessage）
 */
export interface ToolExecutionRecord {
    toolName: string
    arguments?: string
    result?: string
    error?: boolean
    done: boolean
    /** 此工具是否正在等待人工审批 */
    pendingApproval?: boolean
    /** 此工具调用是否被用户拒绝（非错误，用户主动行为） */
    rejected?: boolean
    /** 消息 ID（用于审批场景） */
    msgId?: number
    /** 步骤 ID（智能体多轮迭代定位） */
    stepId?: number | null
    /** 工具调用序号（用于审批 key 唯一标识） */
    seq?: number
}

/**
 * 单条聊天消息
 */
export interface ChatMessage {
    id: string
    /** 后端数据库 ID（仅从接口加载的消息有值，用于游标分页） */
    dbId?: number
    role: MessageRole
    content: string
    thinking?: string
    toolCalls?: ToolExecutionRecord[]
    timestamp: number
    metadata?: ChatMetadata | null
    /** 预渲染的 Markdown 内容 HTML，避免模板中重复 parse */
    renderedContent?: string
    /** 预渲染的 thinking Markdown 内容 HTML */
    renderedThinking?: string
    /** 模型名称（仅 assistant 角色） */
    modelName?: string
    /** 任务计划清单（仅 agent 规划模式的消息有此字段） */
    taskPlan?: import('./agent-chat').AgentTaskPlan | null
    /** 前端展示步骤列表（msg_type=0，按 step ASC 排序，仅历史加载有值） */
    stepList?: import('./agent-chat').AgentMsgStepVO[] | null
    /** 按轮次拆分的独立内容（agent 流式响应时构建，历史加载时回填为单轮次） */
    rounds?: import('./agent-chat').RoundContent[]
}

/**
 * 回答完毕后的元数据
 */
export interface ChatMetadata {
    /** 本轮对话耗时（毫秒） */
    durationMs?: number
    /** 结束时间戳（毫秒） */
    timestamp?: number
    /** prompt token 数 */
    promptTokens?: number
    /** completion token 数 */
    completionTokens?: number
    /** 总 token 数 */
    totalTokens?: number
    /** 缓存读取的 input token 数 */
    cacheReadInputTokens?: number
    /** 缓存写入的 input token 数 */
    cacheWriteInputTokens?: number
    /** 预估费用（美元） */
    costEstimate?: number
    /** 币种代码，如 USD、CNY（为空时默认 $） */
    currency?: string

    [key: string]: unknown
}

/**
 * API 通用响应结构
 * 对应后端 ApiResult
 */
export interface ApiResult<T> {
    code: number
    message: string
    data: T
}
