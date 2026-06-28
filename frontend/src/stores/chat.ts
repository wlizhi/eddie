import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {ChatMessage, ChatMetadata, ChatModelSelector, ToolExecutionRecord} from '@/types/chat'
import type {ToolSourceVO} from '@/types/mcpServer'
import type {MessageVO, ToolExecutionEventItem} from '@/types/session'
import {fetchModelList, streamChat} from '@/api/chat'
import {fetchBoundMcpTools} from '@/api/assistant'
import {createSession, fetchMessages, generateTitle} from '@/api/session'
import {showToast} from '@/composables/useToast'
import {useAssistantStore} from '@/stores/assistant'
import {renderMd} from '@/utils/markdown'

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

/**
 * 聊天 Store
 *
 * 管理：消息列表、模型列表、流式状态、会话生命周期
 */
export const useChatStore = defineStore('chat', () => {
    // ========== 状态 ==========

    /** 消息列表 */
    const messages = ref<ChatMessage[]>([])

    /** 当前会话 ID（数字主键，空字符串 = 新会话） */
    const currentConversationId = ref<string>('')

    /** 模型选择器列表 */
    const modelSelectors = ref<ChatModelSelector[]>([])

    /** 当前选中的模型 ID */
    const currentModelId = ref<string>('')

    /** 当前选中的供应商实例 ID */
    const currentProviderId = ref<number>(0)

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

    /** AbortController，用于中断请求 */
    let abortController: AbortController | null = null

    /** 会话列表刷新计数器（事件驱动侧边栏同步） */
    const sessionRefreshCounter = ref(0)

    /** 当前思考模式：auto / low / medium / high / max / disabled */
    const thinkingMode = ref<string>('auto')

    /** 🌐 联网搜索开关 */
    const webSearchEnabled = ref(false)

    /** 🛠️ MCP 工具模式：disabled / auto / manual */
    const mcpToolMode = ref<'disabled' | 'auto' | 'manual'>('auto')

    /** 手动模式下勾选的 MCP Server ID 列表 */
    const selectedMcpServerIds = ref<number[]>([])

    /** 当前助手已绑定的 MCP + 工具列表缓存（按 sort_order 排序） */
    const boundMcpTools = ref<ToolSourceVO[]>([])

    /** 是否还有更早的消息可加载 */
    const hasMoreMessages = ref(false)

    /** 是否正在加载更早的消息 */
    const isLoadingMore = ref(false)

    /** 每页消息数量（与后端 MESSAGE_PAGE_SIZE 保持一致） */
    const MESSAGE_PAGE_SIZE = 20

    // ========== 计算属性 ==========

    const flatModelOptions = computed(() => {
        const options: { label: string; value: string; group?: string }[] = []
        for (const selector of modelSelectors.value) {
            for (const model of selector.models) {
                options.push({
                    label: model.displayName ?? model.modelId,
                    value: model.modelId,
                    group: selector.providerName,
                })
            }
        }
        return options
    })

    const hasMessages = computed(() => messages.value.length > 0)

    /** 是否为新会话（尚未创建） */
    const isNewConversation = computed(() => currentConversationId.value === '')

    /** 从助手的 thinkingMode 配置同步到当前聊天输入区选择 */
    function syncThinkingMode(mode: string | null): void {
        thinkingMode.value = mode ?? 'auto'
    }

    /**
     * 加载当前助手已绑定的 MCP 工具列表
     */
    async function loadBoundMcpTools(assistantId: number): Promise<void> {
        try {
            boundMcpTools.value = await fetchBoundMcpTools(assistantId)
        } catch (err) {
            console.error('加载绑定 MCP 工具列表失败:', err)
            boundMcpTools.value = []
        }
    }

    /**
     * 根据当前工具模式 + 联网开关，构建发送给后端的工具参数
     *
     * 规则：
     * - MCP禁用 + 联网关 → mode=none
     * - MCP禁用 + 联网开 → mode=manual, tools=BuiltInSearch 下所有工具
     * - MCP手动          → mode=manual, tools=选中 MCP 工具 + 联网工具（去重）
     * - MCP自动          → mode=auto, 不传工具列表
     */
    function buildToolParams(): { toolSelectionMode?: string; toolNames?: string[] } {
        const mode = mcpToolMode.value
        const searchOn = webSearchEnabled.value
        const selectedIds = selectedMcpServerIds.value

        // 查找 BuiltInSearch MCP Server ID（内置搜索）
        const builtInSearch = boundMcpTools.value.find(
            t => t.mcpServerName === 'BuiltInSearch' || t.transportType === 'BUILT_IN'
        )
        const builtInSearchId = builtInSearch?.mcpServerId

        /** 获取指定 MCP Server ID 下的所有工具名称 */
        function getToolNamesByServerId(serverId: number): string[] {
            const server = boundMcpTools.value.find(s => s.mcpServerId === serverId)
            if (!server) return []
            return server.tools.map(t => t.name)
        }

        if (mode === 'disabled') {
            if (searchOn && builtInSearchId != null) {
                // MCP禁用 + 联网开 → 升级为 manual，只传 BuiltInSearch 工具
                return {
                    toolSelectionMode: 'manual',
                    toolNames: getToolNamesByServerId(builtInSearchId),
                }
            }
            return {toolSelectionMode: 'none', toolNames: []}
        }

        if (mode === 'manual') {
            // 合并选中 MCP 的工具 + 联网工具（如果联网开）
            const idsToUse = [...selectedIds]
            if (searchOn && builtInSearchId != null && !idsToUse.includes(builtInSearchId)) {
                idsToUse.push(builtInSearchId)
            }
            const toolNames = idsToUse.flatMap(id => getToolNamesByServerId(id))
            // 去重
            return {
                toolSelectionMode: 'manual',
                toolNames: [...new Set(toolNames)],
            }
        }

        // mode === 'auto' → 不传工具列表，AI 自主决定
        return {toolSelectionMode: 'auto'}
    }

    async function loadModels(): Promise<void> {
        try {
            const list = await fetchModelList()
            modelSelectors.value = list
            if (!currentModelId.value && list.length > 0 && list[0].models.length > 0) {
                const first = list[0].models[0]
                currentModelId.value = first.modelId
                currentProviderId.value = first.providerId
            }
        } catch (err) {
            console.error('加载模型列表失败:', err)
        }
    }

    function selectModel(modelId: string, providerId: number): void {
        currentModelId.value = modelId
        currentProviderId.value = providerId
    }

    /**
     * 发送消息
     *
     * 新会话：先创建 session → 发送消息 → 生成标题
     * 已有会话：直接发送消息
     */
    async function sendMessage(text: string): Promise<void> {
        if (!text.trim() || isStreaming.value) return

        const isFirstRound = isNewConversation.value

        // 新会话：创建 session
        if (isFirstRound) {
            const assistantStore = useAssistantStore()
            if (!assistantStore.activeId) {
                console.error('没有选中的助手')
                return
            }
            try {
                const session = await createSession({assistantId: assistantStore.activeId})
                currentConversationId.value = String(session.id)
            } catch (err) {
                console.error('创建会话失败:', err)
                return
            }
        }

        // 添加用户消息
        const userMsg: ChatMessage = {
            id: generateId(),
            role: 'user',
            content: text.trim(),
            timestamp: Date.now(),
        }
        messages.value.push(userMsg)

        // 重置流式状态
        isStreaming.value = true
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
        currentToolExecutions.value = []

        // 创建 assistant 空消息占位
        const assistantMsg: ChatMessage = {
            id: generateId(),
            role: 'assistant',
            content: '',
            thinking: '',
            timestamp: Date.now(),
        }
        messages.value.push(assistantMsg)

        abortController = new AbortController()

        // 构建工具参数
        const toolParams = buildToolParams()

        await streamChat({
            request: {
                conversationId: currentConversationId.value,
                message: text.trim(),
                providerId: currentProviderId.value,
                modelId: currentModelId.value,
                thinkingMode: thinkingMode.value !== 'auto' ? thinkingMode.value : undefined,
                ...toolParams,
            },
            signal: abortController.signal,
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
            onMetadata: (json) => {
                try {
                    currentMetadata.value = JSON.parse(json) as ChatMetadata
                    const last = messages.value[messages.value.length - 1]
                    if (last && last.role === 'assistant') {
                        last.metadata = currentMetadata.value
                    }
                } catch {
                    // ignore
                }
            },
            onComplete: () => {
                // 将流式工具执行记录赋给最后一条 assistant 消息
                if (currentToolExecutions.value.length > 0) {
                    const last = messages.value[messages.value.length - 1]
                    if (last?.role === 'assistant') {
                        last.toolCalls = [...currentToolExecutions.value]
                    }
                }
                // 清空流式工具记录，避免 MessageList 中 Section B 重复渲染
                currentToolExecutions.value = []
                isStreaming.value = false
                abortController = null
                // 首轮对话后生成标题
                if (isFirstRound) {
                    generateTitleAsync()
                }
                // 刷新会话列表（消息数、更新时间等统计数据）
                sessionRefreshCounter.value++
            },
            onError: (error) => {
                console.error('流式请求出错:', error)
                isStreaming.value = false
                abortController = null
                showToast(error.message, 'error')
            },
        })
    }

    /** 异步生成标题（首轮对话后） */
    async function generateTitleAsync(): Promise<void> {
        const sid = Number(currentConversationId.value)
        if (!sid) return
        try {
            await generateTitle(sid, {
                providerId: currentProviderId.value,
                modelCode: currentModelId.value,
            })
            sessionRefreshCounter.value++
        } catch (err) {
            console.error('生成标题失败:', err)
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
        sendMessage(prevMsg.content)
    }

    function abortStream(): void {
        if (abortController) {
            abortController.abort()
            abortController = null
        }
    }

    /** 新建会话 */
    function newConversation(): void {
        currentConversationId.value = ''
        messages.value = []
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
        hasMoreMessages.value = false
        isLoadingMore.value = false
    }

    /** 切换到已有会话并加载历史消息 */
    async function loadConversation(sessionId: number): Promise<void> {
        currentConversationId.value = String(sessionId)
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
        isLoadingMore.value = false

        try {
            const list = await fetchMessages(sessionId)
            // 后端返回倒序（最新在前），反转为正序后一次性赋值，
            // 避免逐条 push 触发多次 watcher 和中间态的 scrollToBottom
            const newMessages: ChatMessage[] = []
            for (let i = list.length - 1; i >= 0; i--) {
                newMessages.push(toChatMessage(list[i]))
            }
            messages.value = newMessages
            // 返回数量等于每页大小，说明可能还有更多消息
            hasMoreMessages.value = list.length >= MESSAGE_PAGE_SIZE
        } catch (err) {
            messages.value = []
            console.error('加载历史消息失败:', err)
        }
    }

    /**
     * 加载更早的消息（游标分页，向上滚动触发）
     *
     * 使用当前消息列表中最早一条有 dbId 的消息的 ID 作为 beforeId 游标。
     * 后端返回倒序（最新在前），需要反转为正序后前置插入到 messages 头部。
     */
    async function loadMoreMessages(): Promise<void> {
        const sid = Number(currentConversationId.value)
        if (!sid || isLoadingMore.value || !hasMoreMessages.value) return

        // 找到最早一条有 dbId 的消息（消息列表按时间正序排列，最早的在最前面）
        let beforeId: number | undefined
        for (const msg of messages.value) {
            if (msg.dbId != null) {
                beforeId = msg.dbId
                break
            }
        }
        if (beforeId == null) {
            hasMoreMessages.value = false
            return
        }

        isLoadingMore.value = true

        try {
            const list = await fetchMessages(sid, beforeId)
            if (list.length === 0) {
                hasMoreMessages.value = false
                return
            }

            // 后端返回倒序（最新在前），反转为正序后前置插入
            const olderMessages: ChatMessage[] = []
            for (let i = list.length - 1; i >= 0; i--) {
                olderMessages.push(toChatMessage(list[i]))
            }
            messages.value = [...olderMessages, ...messages.value]

            // 返回数量少于每页大小，说明没有更多了
            if (list.length < MESSAGE_PAGE_SIZE) {
                hasMoreMessages.value = false
            }
        } catch (err) {
            console.error('加载更多消息失败:', err)
        } finally {
            isLoadingMore.value = false
        }
    }

    /** MessageVO → ChatMessage（同时预渲染 Markdown 内容，避免模板中重复 parse） */
    function toChatMessage(msg: MessageVO): ChatMessage {
        let toolCalls: ToolExecutionRecord[] | undefined
        if (msg.toolCalls && msg.toolCalls.length > 0) {
            toolCalls = msg.toolCalls.map((tc: ToolExecutionEventItem) => ({
                toolName: tc.toolName,
                arguments: tc.arguments,
                result: tc.result,
                error: tc.error,
                done: tc.status === 'complete',
            }))
        }
        return {
            id: String(msg.id),
            dbId: msg.id,
            role: msg.role as 'user' | 'assistant',
            content: msg.content,
            renderedContent: renderMd(msg.content),
            thinking: msg.thinking || undefined,
            renderedThinking: msg.thinking ? renderMd(msg.thinking) : undefined,
            toolCalls,
            timestamp: new Date(msg.createdAt).getTime(),
            metadata: {
                timestamp: new Date(msg.createdAt).getTime(),
                ...(msg.totalTokens != null ? {totalTokens: msg.totalTokens} : {}),
                ...(msg.promptTokens != null ? {promptTokens: msg.promptTokens} : {}),
                ...(msg.completionTokens != null ? {completionTokens: msg.completionTokens} : {}),
                ...(msg.cacheReadInputTokens != null ? {cacheReadInputTokens: msg.cacheReadInputTokens} : {}),
                ...(msg.cacheWriteInputTokens != null ? {cacheWriteInputTokens: msg.cacheWriteInputTokens} : {}),
                ...(msg.priceEstimate != null ? {costEstimate: msg.priceEstimate} : {}),
                ...(msg.currency ? {currency: msg.currency} : {}),
            },
        }
    }

    return {
        messages,
        currentConversationId,
        modelSelectors,
        currentModelId,
        currentProviderId,
        isStreaming,
        currentThinking,
        currentAnswer,
        currentMetadata,
        currentToolExecutions,
        thinkingMode,
        webSearchEnabled,
        mcpToolMode,
        selectedMcpServerIds,
        boundMcpTools,
        sessionRefreshCounter,
        hasMoreMessages,
        isLoadingMore,
        flatModelOptions,
        hasMessages,
        isNewConversation,
        loadModels,
        selectModel,
        sendMessage,
        regenerate,
        abortStream,
        syncThinkingMode,
        newConversation,
        loadConversation,
        loadMoreMessages,
        loadBoundMcpTools,
    }
})
