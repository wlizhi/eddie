/**
 * useSessionList — 会话列表加载、搜索、过滤
 *
 * 来源：ChatSidebar.vue 会话管理逻辑
 */
import {computed, type ComputedRef, ref, watch} from 'vue'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {deleteSession, fetchSessionList, pinSession, unpinSession} from '@/api/session'
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

    /** 会话列表过滤（支持按标题搜索） */
    const filteredSessions = computed(() => {
        const q = searchQuery.value.trim().toLowerCase()
        if (!q) return sessions.value
        return sessions.value.filter(s => (s.title || '').toLowerCase().includes(q))
    })

    /** 加载会话列表 */
    async function loadSessions() {
        if (!assistantStore.activeId) {
            sessions.value = []
            return
        }
        sessionsLoading.value = true
        try {
            sessions.value = await fetchSessionList(assistantStore.activeId)
        } catch (err) {
            console.error('加载会话列表失败:', err)
        } finally {
            sessionsLoading.value = false
        }
    }

    // 切换助手时重新加载会话列表
    watch(activeId, () => {
        loadSessions()
    }, {immediate: true})

    // 会话 ID 变更时刷新列表
    watch(currentConversationId, (newId) => {
        if (newId) {
            loadSessions()
        }
    })

    // 事件驱动刷新
    watch(sessionRefreshCounter, () => {
        loadSessions()
    })

    /** 删除会话 */
    async function removeSession(sessionId: number) {
        try {
            await deleteSession(sessionId)
            sessions.value = sessions.value.filter(s => s.id !== sessionId)
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
            loadSessions()
        } catch (err) {
            console.error('置顶操作失败:', err)
        }
    }

    return {
        searchQuery,
        sessions,
        sessionsLoading,
        filteredSessions,
        loadSessions,
        removeSession,
        togglePin,
    }
}
