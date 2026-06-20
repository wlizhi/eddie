import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {ChatMessage, ChatMetadata, ChatModelSelector} from '@/types/chat'
import {fetchModelList, streamChat} from '@/api/chat'

/**
 * 聊天 Store
 *
 * 管理：消息列表、模型列表、流式状态
 */
export const useChatStore = defineStore('chat', () => {
    // ========== 状态 ==========

    /** 消息列表 */
    const messages = ref<ChatMessage[]>([])

    /** 当前会话 ID（临时生成） */
    const currentConversationId = ref<string>(crypto.randomUUID())

    /** 模型选择器列表 */
    const modelSelectors = ref<ChatModelSelector[]>([])

    /** 当前选中的模型 ID */
    const currentModelId = ref<string>('')

    /** 当前选中的供应商 code */
    const currentProviderCode = ref<string>('')

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

    /** 展平的模型选项列表（用于 NSelect） */
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

    /** 是否有已发送的消息 */
    const hasMessages = computed(() => messages.value.length > 0)

    // ========== 方法 ==========

    /** 加载模型列表 */
    async function loadModels(): Promise<void> {
        try {
            const list = await fetchModelList()
            modelSelectors.value = list

            // 如果还没有选中的模型，默认选中第一个
            if (!currentModelId.value && list.length > 0 && list[0].models.length > 0) {
                const first = list[0].models[0]
                currentModelId.value = first.modelId
                currentProviderCode.value = first.providerCode
            }
        } catch (err) {
            console.error('加载模型列表失败:', err)
        }
    }

    /** 选择模型（同时更新 modelId 和 providerCode） */
    function selectModel(modelId: string, providerCode: string): void {
        currentModelId.value = modelId
        currentProviderCode.value = providerCode
    }

    /** 发送消息 */
    async function sendMessage(text: string): Promise<void> {
        if (!text.trim() || isStreaming.value) return

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

        // 创建新的 assistant 空消息占位
        const assistantMsg: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: '',
            thinking: '',
            timestamp: Date.now(),
        }
        messages.value.push(assistantMsg)

        // 创建 AbortController
        abortController = new AbortController()

        await streamChat({
            request: {
                conversationId: currentConversationId.value,
                message: text.trim(),
                providerCode: currentProviderCode.value,
                modelId: currentModelId.value,
            },
            signal: abortController.signal,
            onThinking: (chunk) => {
                currentThinking.value += chunk
                // 更新 assistant 消息的 thinking
                const last = messages.value[messages.value.length - 1]
                if (last && last.role === 'assistant') {
                    last.thinking = currentThinking.value
                }
            },
            onAnswer: (chunk) => {
                currentAnswer.value += chunk
                // 更新 assistant 消息的 content（打字机效果）
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
                    // ignore parse error
                }
            },
            onComplete: () => {
                isStreaming.value = false
                abortController = null
            },
            onError: (error) => {
                console.error('流式请求出错:', error)
                isStreaming.value = false
                abortController = null
            },
        })
    }

    /** 中断当前请求 */
    function abortStream(): void {
        if (abortController) {
            abortController.abort()
            abortController = null
        }
    }

    /** 新建会话 */
    function newConversation(): void {
        currentConversationId.value = crypto.randomUUID()
        messages.value = []
        currentThinking.value = ''
        currentAnswer.value = ''
        currentMetadata.value = null
    }

    return {
        // 状态
        messages,
        currentConversationId,
        modelSelectors,
        currentModelId,
        currentProviderCode,
        isStreaming,
        currentThinking,
        currentAnswer,
        currentMetadata,
        // 计算属性
        flatModelOptions,
        hasMessages,
        // 方法
        loadModels,
        selectModel,
        sendMessage,
        abortStream,
        newConversation,
    }
})
