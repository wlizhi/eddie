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
 *   plan_started   — 规划开始（任务清单生成中）
 *   plan_generated — 规划生成成功（任务清单首次生成完毕）
 *   update_task_plan— 更新任务清单（全量任务清单内容）
 *   round_start    — 新一轮迭代开始
 *   metadata       — 执行完毕元数据
 *   message_created— 消息已持久化
 *   cancelled      — 用户停止回答
 *   error          — 服务端错误
 */
import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {ChatMessage, ChatMetadata, ChatModelSelector, ToolExecutionRecord} from '@/types/chat'
import type {AgentTaskPlan} from '@/types/agent-chat'
import type {SessionVO} from '@/types/session'
import {useAgentStore} from '@/stores/agent'
import type {ToolSourceVO} from '@/types/mcpServer'
import {fetchAgentMessages, stopAgentChat, streamAgentChat} from '@/api/agent-chat'
import {createAgentSession, generateAgentSessionTitle} from '@/api/agent-session'
import {fetchAgentBoundMcpTools} from '@/api/agent'
import {showToast} from '@/composables/useToast'
import {
    generateId,
    debounceRender,
    toChatMessage,
    renderFinalContent,
    findOrCreateStep,
} from './agent-chat-helpers'

export const useAgentChatStore = defineStore('agentChat', () => {
    const agentStore = useAgentStore()

    // ========== 状态 ==========

    /** 消息列表 */
    const messages = ref<ChatMessage[]>([])

    /** 当前会话 ID（数字主键，空字符串 = 新会话） */
    const currentConversationId = ref<string>('')

    /** 最近创建的会话（驱动侧边栏本地追加到列表顶部） */
    const lastCreatedSession = ref<SessionVO | null>(null)

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

    /** 手动模式下勾选的工具名列表 */
    const selectedToolNames = ref<string[]>([])

    /** 当前智能体已绑定的 MCP + 工具列表缓存 */
    const boundMcpTools = ref<ToolSourceVO[]>([])

    /**
     * 当前智能体绑定的 BuiltInSearch 下是否存在已启用的联网工具
     * 用于控制联网按钮的置灰状态
     */
    const canWebSearch = computed(() => {
        const builtInSearch = boundMcpTools.value.find(
            t => t.mcpServerName === 'BuiltInSearch'
        )
        if (!builtInSearch?.tools) return false
        return builtInSearch.tools.some(
            t => t.toolType === 'BUILT_IN' && t.enabled
        )
    })

    /**
     * 获取 BuiltInSearch 下已启用的联网工具名
     */
    function getEnabledWebToolNames(): string[] {
        const builtInSearch = boundMcpTools.value.find(
            t => t.mcpServerName === 'BuiltInSearch'
        )
        if (!builtInSearch?.tools) return []
        return builtInSearch.tools
            .filter(t => t.toolType === 'BUILT_IN' && t.enabled)
            .map(t => t.name)
    }

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
            const msgs = list.map(toChatMessage)
            messages.value = msgs
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
     * 按后端 DB ID 查找消息（用于 msgId 定位正确的气泡，而非总是取最后一条）
     * 从数组末尾往前搜索——在流式场景中目标消息总是在最近创建的几条中。
     */
    function findMsgByDbId(dbId?: number | null): ChatMessage | undefined {
        if (dbId == null) return undefined
        for (let i = messages.value.length - 1; i >= 0; i--) {
            if (messages.value[i].dbId === dbId) return messages.value[i]
        }
        return undefined
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
                thinkingMode: thinkingMode.value !== 'auto' ? thinkingMode.value : undefined,
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
                        dbId: data.assistantMsgId,
                        role: 'assistant',
                        content: '',
                        thinking: '',
                        timestamp: Date.now(),
                    })
                } else if (data.assistantMsgId) {
                    // 仅 assistantMsgId → 子任务副消息，只创建 agent slot
                    messages.value.push({
                        id: generateId(),
                        dbId: data.assistantMsgId,
                        role: 'assistant',
                        content: '',
                        thinking: '',
                        timestamp: Date.now(),
                    })
                }
            },
            onThinking: (chunk, _stepNumber, msgId, stepRecordId) => {
                currentThinking.value += chunk
                const target = msgId ? findMsgByDbId(msgId) : undefined
                const msg = target ?? messages.value[messages.value.length - 1]
                if (msg && msg.role === 'assistant') {
                    msg.thinking = currentThinking.value
                    // 按 stepRecordId 定位步骤，追加思考内容
                    const step = findOrCreateStep(msg, stepRecordId)
                    // 检查动画状态，不是动画则改为动画
                    if (!step.thinkingStreaming) {
                        step.thinkingStreaming = true
                    }
                    step.thinking += chunk
                }
            },
            onAnswer: (chunk, _stepNumber, msgId, stepRecordId) => {
                currentAnswer.value += chunk
                const target = msgId ? findMsgByDbId(msgId) : undefined
                const msg = target ?? messages.value[messages.value.length - 1]
                if (msg && msg.role === 'assistant') {
                    msg.content = currentAnswer.value
                    debounceRender(msg)
                    // 按 stepRecordId 定位步骤，追加回答内容
                    const step = findOrCreateStep(msg, stepRecordId)
                    step.content += chunk
                }
            },
            onToolExecution: (data, _stepNumber, _msgId2, _stepRecordId2) => {
                const msgId = data.msgId
                const stepRecordId = data.stepRecordId
                const seq = data.seq ?? 0

                /** 按 msgId + stepRecordId + toolName + !done 定位匹配的 tool record */
                const findTool = (arr: ToolExecutionRecord[]) => arr.find(t =>
                    t.toolName === data.toolName &&
                    t.msgId === msgId &&
                    t.stepRecordId === stepRecordId &&
                    !t.done
                )

                if (data.status === 'start') {
                    const toolRec: ToolExecutionRecord = {
                        toolName: data.toolName!,
                        arguments: data.arguments,
                        done: false,
                        msgId,
                        stepRecordId,
                        seq,
                    }
                    // 记录到 currentToolExecutions（兼容旧引用）
                    currentToolExecutions.value = [...currentToolExecutions.value, toolRec]
                    // 按 stepRecordId 定位步骤，追加工具调用
                    const last = messages.value[messages.value.length - 1]
                    if (last?.role === 'assistant') {
                        const step = findOrCreateStep(last, stepRecordId)
                        step.toolCalls.push(toolRec)
                    }
                } else if (data.status === 'pending_approval') {
                    const found = findTool(currentToolExecutions.value)
                    if (found) {
                        found.pendingApproval = true
                        found.arguments = data.arguments
                    }
                    // 同步更新步骤中的 toolCall
                    const last = messages.value[messages.value.length - 1]
                    if (last?.role === 'assistant') {
                        const step = findOrCreateStep(last, stepRecordId)
                        const stepTool = findTool(step.toolCalls)
                        if (stepTool) {
                            stepTool.pendingApproval = true
                            stepTool.arguments = data.arguments
                        }
                    }
                } else if (data.status === 'complete' || data.status === 'rejected') {
                    const found = findTool(currentToolExecutions.value)
                    if (found) {
                        found.result = data.result
                        found.error = data.status === 'rejected' ? false : !!data.error
                        found.done = true
                        found.pendingApproval = false
                        found.rejected = data.status === 'rejected'
                    }
                    // 同步更新步骤中的 toolCall
                    const last = messages.value[messages.value.length - 1]
                    if (last?.role === 'assistant') {
                        const step = findOrCreateStep(last, stepRecordId)
                        const stepTool = findTool(step.toolCalls)
                        if (stepTool) {
                            stepTool.result = data.result
                            stepTool.error = data.status === 'rejected' ? false : !!data.error
                            stepTool.done = true
                            stepTool.pendingApproval = false
                            stepTool.rejected = data.status === 'rejected'
                        }
                    }
                }
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
                    // MetadataPayload: { msgId, stepRecordId, stepNumber, stats }
                    // 提取 stats 展开为扁平结构以匹配 ChatMetadata 接口
                    const payload = JSON.parse(json) as Record<string, unknown>
                    const stats = (payload?.stats ?? {}) as Record<string, unknown>
                    currentMetadata.value = {
                        promptTokens: stats.promptTokens as number | undefined,
                        completionTokens: stats.completionTokens as number | undefined,
                        totalTokens: stats.totalTokens as number | undefined,
                        cacheReadInputTokens: stats.cacheReadInputTokens as number | undefined,
                        cacheWriteInputTokens: stats.cacheWriteInputTokens as number | undefined,
                        // 后端字段 priceEstimate → 前端字段 costEstimate
                        costEstimate: stats.priceEstimate as number | undefined,
                        currency: stats.currency as string | undefined,
                        durationMs: stats.durationMs as number | undefined,
                    } as ChatMetadata
                    // 按 msgId 定位消息，而非总是取最后一条
                    const msgId = payload.msgId as number | null | undefined
                    const target = msgId ? findMsgByDbId(msgId) : undefined
                    const msg = target ?? messages.value[messages.value.length - 1]
                    if (msg && msg.role === 'assistant') {
                        // 关闭思考动画：按 stepRecordId 定位 round
                        const stepRecordId = payload.stepRecordId as number | null | undefined
                        if (msg.rounds) {
                            const round = msg.rounds.find(r => (r.stepRecordId ?? null) === (stepRecordId ?? null))
                            if (round) {
                                round.thinkingStreaming = false
                            }
                        }
                        msg.metadata = currentMetadata.value
                        // metadata 到达时强制最终渲染
                        renderFinalContent(msg)
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
     * 重新生成：取指定助手消息上一条用户消息的内容重新发送
     * @param msgIndex 当前消息在 messages 中的索引
     */
    function regenerate(msgIndex: number): void {
        if (isStreaming.value) return
        const prevMsg = messages.value[msgIndex - 1]
        if (!prevMsg || prevMsg.role !== 'user') return
        const agentId = agentStore.activeAgent?.id
        if (agentId == null) return
        sendMessage(prevMsg.content, agentId)
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
            // 关闭所有 round 的思考动画（兜底，防止 metadata 事件漏处理导致动画永久持续）
            if (last.rounds) {
                last.rounds.forEach(r => { r.thinkingStreaming = false })
            }
            if (currentToolExecutions.value.length > 0) {
                last.toolCalls = [...currentToolExecutions.value]
            }
            // 流结束时强制立即渲染最终内容
            renderFinalContent(last)
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
     *
     * 规则：
     * - MCP禁用 + 联网关 → toolSelectionMode=none
     * - MCP禁用 + 联网开 → toolSelectionMode=manual, tools=BuiltInSearch 下已启用的联网工具
     * - MCP手动          → toolSelectionMode=manual, tools=选中 MCP 中已启用的工具 + 联网工具（去重）
     * - MCP自动          → toolSelectionMode=manual, tools=所有绑定的非禁用工具 + 联网工具（去重）
     * - 工具列表为空时    → toolSelectionMode=none
     */
    function buildToolParams(): { toolSelectionMode?: string; toolNames?: string[] } {
        const mode = mcpToolMode.value
        const searchOn = webSearchEnabled.value

        // 收集工具列表（去重后）
        const toolNames: string[] = []

        if (mode === 'disabled') {
            // Rule 5: MCP 禁用状态不发送工具列表，但联网按钮独立工作
            if (searchOn) {
                toolNames.push(...getEnabledWebToolNames())
            }
        } else if (mode === 'manual') {
            // 直接使用已勾选的工具名列表
            toolNames.push(...selectedToolNames.value)
            // 联网开 → 合并 BuiltInSearch 下已启用的联网工具
            if (searchOn) {
                toolNames.push(...getEnabledWebToolNames())
            }
        } else {
            // mode === 'auto'
            // Rule 6: MCP 自动 → 发送绑定的全部非禁用工具列表 + 联网工具（去重）
            for (const mcp of boundMcpTools.value) {
                toolNames.push(...mcp.tools.filter(t => {
                    // 优先使用 enabledStatus：0=禁用, 1=启用, 2=待审批（均视为可用）
                    if (t.enabledStatus != null) return t.enabledStatus !== 0
                    return t.enabled
                }).map(t => t.name))
            }
            if (searchOn) {
                toolNames.push(...getEnabledWebToolNames())
            }
        }

        // 去重
        const uniqueNames = [...new Set(toolNames)]
        if (uniqueNames.length === 0) {
            return {toolSelectionMode: 'none', toolNames: []}
        }
        return {
            toolSelectionMode: 'manual',
            toolNames: uniqueNames,
        }
    }

    return {
        // 状态
        messages,
        currentConversationId,
        lastCreatedSession,
        isStreaming,
        currentThinking,
        currentAnswer,
        currentMetadata,
        currentToolExecutions,
        currentRound,
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
        canWebSearch,
        mcpToolMode,
        selectedToolNames,
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
        regenerate,
        loadMoreMessages,
        abortStream,
        loadModels,
        loadBoundMcpTools,
    }
})