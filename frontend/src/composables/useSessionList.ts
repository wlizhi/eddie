/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * useSessionList — 会话列表加载、搜索、分页
 *
 * 来源：ChatSidebar.vue 会话管理逻辑
 * 后端改为分页接口后，搜索走服务端，分页采用累加加载模式
 */
import {computed, type ComputedRef, ref, watch} from 'vue'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {deleteSession, fetchSessionList, generateTitle, pinSession, renameTitle, unpinSession} from '@/api/session'
import type {SessionVO} from '@/types/session'

export function useSessionList(
    activeId: ComputedRef<number | null>,
    sessionRefreshCounter: ComputedRef<number>,
    currentConversationId: ComputedRef<string | null>
) {
    const assistantStore = useAssistantStore()
    const chatStore = useChatStore()

    const searchQuery = ref('')
    const sessions = ref<SessionVO[]>([])
    const sessionsLoading = ref(false)
    const loadingMore = ref(false)

    /** 分页状态 */
    const pageNum = ref(1)
    const total = ref(0)
    const hasMore = computed(() => sessions.value.length < total.value)

    /** 防抖搜索定时器 */
    let searchTimer: ReturnType<typeof setTimeout> | null = null

    /**
     * 加载会话列表
     * @param reset true=重新第1页替换列表，false=追加下一页
     */
    async function loadSessions(reset = true) {
        const assistantId = assistantStore.activeId
        if (!assistantId) {
            sessions.value = []
            total.value = 0
            return
        }

        if (reset) {
            pageNum.value = 1
            sessionsLoading.value = true
        } else {
            loadingMore.value = true
        }

        try {
            const title = searchQuery.value.trim() || undefined
            const result = await fetchSessionList(assistantId, pageNum.value, 50, title)
            if (reset) {
                sessions.value = result.records
            } else {
                sessions.value.push(...result.records)
            }
            total.value = result.total
        } catch (err) {
            console.error('加载会话列表失败:', err)
        } finally {
            sessionsLoading.value = false
            loadingMore.value = false
        }
    }

    /** 加载更多（滚动触底） */
    async function loadMore() {
        if (!hasMore.value || loadingMore.value) return
        pageNum.value++
        await loadSessions(false)
    }

    /** 带防抖的搜索 */
    function onSearchChange() {
        if (searchTimer) clearTimeout(searchTimer)
        searchTimer = setTimeout(() => {
            loadSessions(true)
        }, 300)
    }

    // 搜索关键词变化 → 防抖后重新加载（第1页）
    watch(searchQuery, () => {
        onSearchChange()
    })

    // 切换助手时重新加载
    watch(activeId, () => {
        loadSessions(true)
    }, {immediate: true})

    // 会话 ID 变更时刷新列表
    watch(currentConversationId, (newId) => {
        if (newId) {
            loadSessions(true)
        }
    })

    // 事件驱动刷新
    watch(sessionRefreshCounter, () => {
        loadSessions(true)
    })

    /** 删除会话 */
    async function removeSession(sessionId: number) {
        try {
            await deleteSession(sessionId)
            sessions.value = sessions.value.filter(s => s.id !== sessionId)
            total.value = Math.max(0, total.value - 1)
            if (chatStore.currentConversationId === String(sessionId)) {
                chatStore.newConversation()
            }
        } catch (err) {
            console.error('删除会话失败:', err)
        }
    }

    /** 置顶/取消置顶 */
    async function togglePin(session: SessionVO) {
        try {
            if (session.pinned) {
                await unpinSession(session.id)
            } else {
                await pinSession(session.id)
            }
            loadSessions(true)
        } catch (err) {
            console.error('置顶操作失败:', err)
        }
    }

    /** 手动重命名 */
    async function renameSession(sessionId: number, title: string) {
        try {
            await renameTitle(sessionId, {title})
            loadSessions(true)
        } catch (err) {
            console.error('重命名失败:', err)
        }
    }

    /** AI 生成标题 */
    async function aiGenerateTitle(sessionId: number) {
        try {
            await generateTitle(sessionId, {
                providerId: chatStore.currentProviderId,
                modelCode: chatStore.currentModelId,
            })
            loadSessions(true)
        } catch (err) {
            console.error('AI 生成标题失败:', err)
        }
    }

    return {
        searchQuery,
        sessions,
        sessionsLoading,
        loadingMore,
        hasMore,
        loadSessions,
        loadMore,
        removeSession,
        togglePin,
        renameSession,
        aiGenerateTitle,
    }
}
