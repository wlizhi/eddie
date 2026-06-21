import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {ChatMessage, ChatMetadata, ChatModelSelector} from '@/types/chat'
import type {MessageVO} from '@/types/session'
import {fetchModelList, streamChat} from '@/api/chat'
import {createSession, fetchMessages, generateTitle} from '@/api/session'
import {useAssistantStore} from '@/stores/assistant'

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

    /** AbortController，用于中断请求 */
    let abortController: AbortController | null = null

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

    // ========== 方法 ==========

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
            id: crypto.randomUUID(),
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

        // 创建 assistant 空消息占位
        const assistantMsg: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: '',
            thinking: '',
            timestamp: Date.now(),
        }
        messages.value.push(assistantMsg)

        abortController = new AbortController()

        await streamChat({
            request: {
                conversationId: currentConversationId.value,
                message: text.trim(),
                providerId: currentProviderId.value,
                modelId: currentModelId.value,
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
                isStreaming.value = false
                abortController = null
                // 首轮对话后生成标题
                if (isFirstRound) {
                    generateTitleAsync()
                }
            },
            onError: (error) => {
                console.error('流式请求出错:', error)
                isStreaming.value = false
                abortController = null
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
        } catch (err) {
            console.error('生成标题失败:', err)
        }
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
    }

    /** 切换到已有会话并加载历史消息 */
    async function loadConversation(sessionId: number): Promise<void> {
        currentConversationId.value = String(sessionId)
        messages.value = []
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null

        try {
            const list = await fetchMessages(sessionId)
            // 后端返回倒序（最新在前），反转为正序
            for (let i = list.length - 1; i >= 0; i--) {
                messages.value.push(toChatMessage(list[i]))
            }
        } catch (err) {
            console.error('加载历史消息失败:', err)
        }
    }

    /** MessageVO → ChatMessage */
    function toChatMessage(msg: MessageVO): ChatMessage {
        return {
            id: String(msg.id),
            role: msg.role as 'user' | 'assistant',
            content: msg.content,
            thinking: msg.thinking || undefined,
            timestamp: new Date(msg.createdAt).getTime(),
            metadata: msg.totalTokens ? {
                promptTokens: msg.promptTokens,
                completionTokens: msg.completionTokens,
                totalTokens: msg.totalTokens,
            } as ChatMetadata : undefined,
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
        flatModelOptions,
        hasMessages,
        isNewConversation,
        loadModels,
        selectModel,
        sendMessage,
        abortStream,
        newConversation,
        loadConversation,
    }
})
