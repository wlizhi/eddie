/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体聊天 Store
 *
 * 管理：消息列表、流式状态、会话生命周期
 * 与普通聊天 Store 不同，Agent 聊天由后端智能体引擎接管执行流程，
 * 支持多轮迭代、工具调用、任务规划等复杂编排。
 *
 * SSE 事件：
 *   thinking       — 模型思考内容
 *   answer         — 模型回答内容
 *   tool_execution — 工具执行状态
 *   milestone      — 关键里程碑
 *   round_start    — 新一轮迭代开始
 *   metadata       — 执行完毕元数据
 *   message_created— 消息已持久化
 *   cancelled      — 用户停止回答
 *   error          — 服务端错误
 */
import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {ChatMessage, ChatMetadata, ChatModelSelector, ToolExecutionRecord} from '@/types/chat'
import type {AgentTaskPlan, MilestoneEvent} from '@/types/agent-chat'
import type {SessionVO, ToolExecutionEventItem} from '@/types/session'
import {useAgentStore} from '@/stores/agent'
import type {ToolSourceVO} from '@/types/mcpServer'
import {fetchAgentMessages, stopAgentChat, streamAgentChat} from '@/api/agent-chat'
import {createAgentSession, generateAgentSessionTitle} from '@/api/agent-session'
import {fetchAgentBoundMcpTools} from '@/api/agent'
import {renderMd} from '@/utils/markdown'
import {showToast} from '@/composables/useToast'

/**
 * 生成唯一 ID（兼容 Safari 15.4 以下不支持 crypto.randomUUID）
 */
function generateId(): string {
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
function debounceRender(msg: ChatMessage): void {
    cancelAnimationFrame(renderRafId)
    renderRafId = requestAnimationFrame(() => {
        msg.renderedContent = renderMd(msg.content || '')
    })
}

/** 将 AgentMessageVO.toolCalls（JSON 字符串）解析为 ToolExecutionRecord[] */
function parseToolCalls(toolCallsJson: string | null | undefined): ToolExecutionRecord[] {
    if (!toolCallsJson) return []
    try {
        const parsed = JSON.parse(toolCallsJson) as ToolExecutionEventItem[]
        return parsed.map(item => ({
            toolName: item.toolName,
            arguments: item.arguments,
            result: item.result,
            error: item.error,
            done: item.status === 'complete',
        }))
    } catch {
        return []
    }
}

/** 将后端 AgentMessageVO 转换为前端 ChatMessage */
function toChatMessage(vo: {
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
}): ChatMessage {
    const content = vo.content || ''
    return {
        id: generateId(),
        dbId: vo.id,
        role: vo.role as 'user' | 'assistant',
        content,
        renderedContent: content ? renderMd(content) : '',
        thinking: vo.thinking || undefined,
        toolCalls: parseToolCalls(vo.toolCalls),
        timestamp: vo.createdAt,
        modelName: vo.modelName || undefined,
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
}

export const useAgentChatStore = defineStore('agentChat', () => {
    const agentStore = useAgentStore()

    // ========== 状态 ==========

    /** 消息列表 */
    const messages = ref<ChatMessage[]>([])

    /** 当前会话 ID（数字主键，空字符串 = 新会话） */
    const currentConversationId = ref<string>('')

    /** 最近创建的会话（驱动侧边栏本地追加到列表顶部） */
    const lastCreatedSession = ref<SessionVO | null>(null)

    /** 是否正在加载消息 */
    const loadingMessages = ref(false)

    /** 是否正在流式响应中 */
    const isStreaming = ref(false)

    /** 最近的流式 thinking 内容 */
    const currentThinking = ref('')

    /** 最近的流式 answer 内容 */
    const currentAnswer = ref('')

    /** 最近消息的元数据 */
    const currentMetadata = ref<ChatMetadata | null>(null)

    /** 当前流中的工具执行记录列表（每次发送消息时重置） */
    const currentToolExecutions = ref<ToolExecutionRecord[]>([])

    /** 当前迭代轮次 */
    const currentRound = ref(0)

    /** 里程碑事件列表 */
    const milestones = ref<MilestoneEvent[]>([])

    /** 当前任务计划清单（规划模式） */
    const currentTaskPlan = ref<AgentTaskPlan | null>(null)

    /** 是否正在生成任务计划（由后端 plan_started / plan_generated 事件控制） */
    const isPlanGenerating = ref(false)

    /** 最新被后端确认已接收的文本（供 ChatView 清空输入框） */
    const confirmedText = ref('')

    /** AbortController，用于中断请求 */
    let abortController: AbortController | null = null


    /** 当前正在执行的 agent 消息 ID（来自 SSE message_created 事件） */
    let currentAgentMsgId: number | null = null

    /** 会话消息数 +2 信号（模型回复完毕后递增，驱动侧边栏本地更新） */
    const sessionMessageSignal = ref(0)

    /** 是否还有更早的消息可加载 */
    const hasMoreMessages = ref(false)

    /** 是否正在加载更早的消息 */
    const isLoadingMore = ref(false)

    /** 每页消息数量（与后端保持一致） */
    const MESSAGE_PAGE_SIZE = 20

    // ========== 工具栏状态 ==========

    /** 模型选择器列表 */
    const modelSelectors = ref<ChatModelSelector[]>([])

    /** 当前思考模式：auto / low / medium / high / max / disabled */
    const thinkingMode = ref<string>('auto')

    /** 🌐 联网搜索开关 */
    const webSearchEnabled = ref(false)

    /** 🛠️ MCP 工具模式：disabled / auto / manual */
    const mcpToolMode = ref<'disabled' | 'auto' | 'manual'>('auto')

    /** 手动模式下勾选的 MCP Server ID 列表 */
    const selectedMcpServerIds = ref<number[]>([])

    /** 当前智能体已绑定的 MCP + 工具列表缓存 */
    const boundMcpTools = ref<ToolSourceVO[]>([])

    // ==================== 用户临时覆盖参数 ====================

    /** 临时覆盖的主模型服务商 ID（null=使用 Agent 配置，刷新清空） */
    const selectedProviderId = ref<number | null>(null)

    /** 临时覆盖的主模型 ID（null=使用 Agent 配置，刷新清空） */
    const selectedModelId = ref<string | null>(null)

    // ========== 计算属性 ==========

    const hasMessages = computed(() => messages.value.length > 0)

    /** 是否为新会话（尚未创建） */
    const isNewConversation = computed(() => currentConversationId.value === '')

    // ========== 方法 ==========

    /** 新建会话（仅重置本地状态，不调用后端） */
    function newConversation(): void {
        currentConversationId.value = ''
        messages.value = []
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
        currentToolExecutions.value = []
        currentRound.value = 0
        milestones.value = []
        currentTaskPlan.value = null
        isPlanGenerating.value = false
    }

    /**
     * 加载模型选择器列表
     */
    async function loadModels(): Promise<void> {
        try {
            const {fetchModelList} = await import('@/api/chat')
            modelSelectors.value = await fetchModelList()
        } catch (err) {
            console.error('加载模型列表失败:', err)
        }
    }

    /**
     * 加载当前智能体已绑定的 MCP 工具列表
     */
    async function loadBoundMcpTools(agentId: number): Promise<void> {
        try {
            boundMcpTools.value = await fetchAgentBoundMcpTools(agentId)
        } catch (err) {
            console.error('加载智能体 MCP 工具列表失败:', err)
            boundMcpTools.value = []
        }
    }

    /**
     * 选中会话并加载历史消息
     */
    async function loadConversation(sessionId: number): Promise<void> {
        currentConversationId.value = String(sessionId)
        messages.value = []
        hasMoreMessages.value = true
        isLoadingMore.value = true

        try {
            const list = await fetchAgentMessages(sessionId)
            if (list.length === 0) {
                hasMoreMessages.value = false
                return
            }
            // 后端返回正序，直接赋值
            messages.value = list.map(toChatMessage)
            if (list.length < MESSAGE_PAGE_SIZE) {
                hasMoreMessages.value = false
            }
        } catch (err) {
            console.error('加载智能体会话消息失败:', err)
        } finally {
            isLoadingMore.value = false
        }
    }

    /**
     * 创建并选中新会话
     *
     * @param agentId 智能体 ID
     * @returns 创建后的会话，失败返回 null
     */
    async function createAndSelectSession(agentId: number): Promise<SessionVO | null> {
        try {
            const session = await createAgentSession(agentId)
            currentConversationId.value = String(session.id)
            lastCreatedSession.value = session
            messages.value = []
            hasMoreMessages.value = false
            return session
        } catch (err) {
            console.error('创建智能体会话失败:', err)
            return null
        }
    }

    /**
     * 发送消息
     *
     * 新会话：先创建 session → 发送消息
     * 已有会话：直接发送消息
     */
    async function sendMessage(text: string, agentId: number): Promise<void> {
        if (!text.trim() || isStreaming.value) return

        const isFirstRound = isNewConversation.value
        const rawText = text.trim()

        // 新会话：创建 session
        if (isFirstRound) {
            try {
                const session = await createAgentSession(agentId)
                currentConversationId.value = String(session.id)
                lastCreatedSession.value = session
            } catch (err) {
                console.error('创建智能体会话失败:', err)
                return
            }
        }

        // 重置流式状态
        isStreaming.value = true
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
        currentToolExecutions.value = []
        currentRound.value = 0
        milestones.value = []
        currentTaskPlan.value = null
        isPlanGenerating.value = false

        abortController = new AbortController()

        // 构建工具参数（根据 MCP 工具模式 + 联网开关）
        const toolParams = buildToolParams()

        const agent = agentStore.activeAgent

        await streamAgentChat({
            request: {
                agentId,
                conversationId: Number(currentConversationId.value) || undefined,
                message: rawText,
                ...toolParams,
                // 用户临时覆盖参数 > Agent 配置（确保请求体始终包含模型信息）
                providerId: selectedProviderId.value ?? agent?.mainProviderId ?? undefined,
                modelId: selectedModelId.value ?? agent?.mainModelId ?? undefined,
                webSearchEnabled: webSearchEnabled.value || undefined,
            },
            signal: abortController.signal,
            onMessageCreated: (data) => {
                // 缓存 assistantMsgId，供停止请求使用
                if (data.assistantMsgId) {
                    currentAgentMsgId = data.assistantMsgId
                }

                // 不含任何消息 ID → 非消息创建事件（如 agent 初始化确认），忽略
                if (!data.userMsgId && !data.assistantMsgId) return

                if (data.userMsgId) {
                    confirmedText.value = rawText
                    messages.value.push({
                        id: generateId(),
                        role: 'user',
                        content: rawText,
                        timestamp: Date.now(),
                    })
                    messages.value.push({
                        id: generateId(),
                        role: 'assistant',
                        content: '',
                        thinking: '',
                        timestamp: Date.now(),
                    })
                } else if (data.assistantMsgId) {
                    // 仅 assistantMsgId → 子任务副消息，只创建 agent slot
                    messages.value.push({
                        id: generateId(),
                        role: 'assistant',
                        content: '',
                        thinking: '',
                        timestamp: Date.now(),
                    })
                }
            },
            onThinking: (chunk) => {
                currentThinking.value += chunk
                const last = messages.value[messages.value.length - 1]
                if (last && last.role === 'assistant') {
                    last.thinking = currentThinking.value
                }
            },
            onAnswer: (chunk) => {
                currentAnswer.value += chunk
                const last = messages.value[messages.value.length - 1]
                if (last && last.role === 'assistant') {
                    last.content = currentAnswer.value
                    debounceRender(last)
                }
            },
            onToolExecution: (data) => {
                if (data.status === 'start') {
                    currentToolExecutions.value.push({
                        toolName: data.toolName,
                        arguments: data.arguments,
                        done: false,
                    })
                } else if (data.status === 'complete') {
                    const existing = currentToolExecutions.value.find(
                        t => t.toolName === data.toolName && !t.done
                    )
                    if (existing) {
                        existing.result = data.result
                        existing.error = data.error
                        existing.done = true
                    } else {
                        currentToolExecutions.value.push({
                            toolName: data.toolName,
                            result: data.result,
                            error: data.error,
                            done: true,
                        })
                    }
                }
            },
            onMilestone: (event) => {
                milestones.value.push(event)
                // 里程碑也附加到最后一条 assistant 消息中（可选增强）
            },
            onPlanStarted: () => {
                isPlanGenerating.value = true
            },
            onPlanGenerated: (plan) => {
                isPlanGenerating.value = false
                currentTaskPlan.value = plan
                // 同时附加到最后一条 assistant 消息
                const last = messages.value[messages.value.length - 1]
                if (last?.role === 'assistant') {
                    last.taskPlan = plan
                }
            },
            onTaskPlan: (plan) => {
                currentTaskPlan.value = plan
                // 同时附加到最后一条 assistant 消息
                const last = messages.value[messages.value.length - 1]
                if (last?.role === 'assistant') {
                    last.taskPlan = plan
                }
            },
            onRoundStart: (round) => {
                currentRound.value = round
            },
            onMetadata: (json) => {
                try {
                    currentMetadata.value = JSON.parse(json) as ChatMetadata
                    const last = messages.value[messages.value.length - 1]
                    if (last && last.role === 'assistant') {
                        last.metadata = currentMetadata.value
                        // metadata 到达时强制最终渲染
                        cancelAnimationFrame(renderRafId)
                        last.renderedContent = renderMd(last.content || '')
                    }
                } catch {
                    // ignore
                }
            },
            onCancelled: () => {
                finishStream()
            },
            onComplete: () => {
                finishStream()
            },
            onError: (error) => {
                console.error('Agent 流式请求出错:', error)
                isStreaming.value = false
                abortController = null
                showToast(error.message, 'error')
            },
        })
    }

    /**
     * 加载更多历史消息（游标分页，滚动到顶部时触发）
     */
    async function loadMoreMessages(): Promise<void> {
        if (isLoadingMore.value || !hasMoreMessages.value) return
        const sid = Number(currentConversationId.value)
        if (!sid) return

        isLoadingMore.value = true
        // 取最早消息的 dbId 作为游标
        const earliest = messages.value.reduce<number | undefined>((min, msg) => {
            return msg.dbId != null && (min == null || msg.dbId < min) ? msg.dbId : min
        }, undefined)

        try {
            const list = await fetchAgentMessages(sid, earliest, MESSAGE_PAGE_SIZE)
            if (list.length === 0) {
                hasMoreMessages.value = false
                return
            }
            // 后端返回正序，前置插入
            const olderMessages = list.map(toChatMessage)
            messages.value = [...olderMessages, ...messages.value]

            if (list.length < MESSAGE_PAGE_SIZE) {
                hasMoreMessages.value = false
            }
        } catch (err) {
            console.error('加载更多智能体消息失败:', err)
        } finally {
            isLoadingMore.value = false
        }
    }

    /**
     * 停止智能体执行
     */
    function abortStream(): void {
        if (!abortController) return

        if (currentAgentMsgId) {
            stopAgentChat(currentAgentMsgId).catch(() => {
                // stop 请求失败时，降级为前端主动断开
                abortController?.abort()
                abortController = null
                isStreaming.value = false
            })
        }
    }

    /**
     * 流完成/取消后的统一清理
     */
    function finishStream(): void {
        // 将流式工具执行记录赋给最后一条 agent 消息
        const last = messages.value[messages.value.length - 1]
        if (last?.role === 'assistant') {
            if (currentToolExecutions.value.length > 0) {
                last.toolCalls = [...currentToolExecutions.value]
            }
            // 流结束时强制立即渲染最终内容
            cancelAnimationFrame(renderRafId)
            last.renderedContent = renderMd(last.content || '')
        }
        // 断开 SSE 连接
        if (abortController) {
            abortController.abort()
        }
        // 清空流式工具记录
        currentToolExecutions.value = []
        isStreaming.value = false
        isPlanGenerating.value = false
        abortController = null
        // 通知侧边栏本地更新当前会话的消息数（+2）和更新时间
        sessionMessageSignal.value++

        // 首轮对话后自动生成标题（2 条消息：1 user + 1 assistant）
        if (messages.value.length === 2) {
            const sid = Number(currentConversationId.value)
            if (sid) {
                generateAgentSessionTitle(sid).catch(err => {
                    console.error('自动生成智能体会话标题失败:', err)
                })
            }
        }
    }

    /**
     * 根据当前工具模式 + 联网开关，构建发送给后端的工具参数
     */
    function buildToolParams(): { toolSelectionMode?: string; toolNames?: string[] } {
        const mode = mcpToolMode.value
        const searchOn = webSearchEnabled.value

        if (mode === 'disabled') {
            if (searchOn) {
                return {toolSelectionMode: 'none', toolNames: []}
            }
            return {toolSelectionMode: 'none', toolNames: []}
        }

        if (mode === 'manual') {
            const toolNames: string[] = []
            for (const mcp of boundMcpTools.value) {
                if (selectedMcpServerIds.value.includes(mcp.mcpServerId)) {
                    toolNames.push(...mcp.tools.map(t => t.name))
                }
            }
            return {
                toolSelectionMode: 'manual',
                toolNames: [...new Set(toolNames)],
            }
        }

        // mode === 'auto'
        return {toolSelectionMode: 'auto'}
    }

    return {
        // 状态
        messages,
        currentConversationId,
        lastCreatedSession,
        loadingMessages,
        isStreaming,
        currentThinking,
        currentAnswer,
        currentMetadata,
        currentToolExecutions,
        currentRound,
        milestones,
        currentTaskPlan,
        isPlanGenerating,
        confirmedText,
        sessionMessageSignal,
        hasMoreMessages,
        isLoadingMore,

        // 工具栏状态
        modelSelectors,
        thinkingMode,
        webSearchEnabled,
        mcpToolMode,
        selectedMcpServerIds,
        boundMcpTools,

        // 用户临时覆盖参数
        selectedProviderId,
        selectedModelId,

        // 计算属性
        hasMessages,
        isNewConversation,

        // 方法
        newConversation,
        loadConversation,
        createAndSelectSession,
        sendMessage,
        loadMoreMessages,
        abortStream,
        loadModels,
        loadBoundMcpTools,
    }
})

