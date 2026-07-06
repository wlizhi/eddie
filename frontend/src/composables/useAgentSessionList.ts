/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * useAgentSessionList — 智能体会话列表加载、搜索、分页
 *
 * 复刻 useSessionList，但使用 agent 的 API 和 Store
 */
import {computed, type ComputedRef, ref, watch} from 'vue'
import {useAgentStore} from '@/stores/agent'
import {useAgentChatStore} from '@/stores/agent-chat'
import {
    deleteAgentSession,
    fetchAgentSessionList,
    generateAgentSessionTitle,
    pinAgentSession,
    renameAgentSessionTitle,
    unpinAgentSession
} from '@/api/agent-session'
import type {SessionVO} from '@/types/session'

export function useAgentSessionList(
    activeId: ComputedRef<number | null>
) {
    const agentStore = useAgentStore()
    const agentChatStore = useAgentChatStore()

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
        const agentId = agentStore.activeId
        if (!agentId) {
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
            const result = await fetchAgentSessionList(agentId, pageNum.value, 50, title)
            if (reset) {
                sessions.value = result.records
            } else {
                sessions.value.push(...result.records)
            }
            total.value = result.total
        } catch (err) {
            console.error('加载智能体会话列表失败:', err)
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

    // 切换智能体时重新加载
    watch([activeId, () => agentStore.loaded], () => {
        if (agentStore.activeId && agentStore.loaded) {
            loadSessions(true)
        }
    }, {immediate: true})

    // 新建会话时，直接追加到列表顶部，避免全量刷新
    watch(() => agentChatStore.lastCreatedSession, (session) => {
        if (session === null) return
        const exists = sessions.value.some(s => s.id === session.id)
        if (!exists) {
            sessions.value.unshift(session)
            total.value++
        }
    })

    /** 删除会话 */
    async function removeSession(sessionId: number) {
        try {
            await deleteAgentSession(sessionId)
            sessions.value = sessions.value.filter(s => s.id !== sessionId)
            total.value = Math.max(0, total.value - 1)
            if (agentChatStore.currentConversationId === String(sessionId)) {
                agentChatStore.newConversation()
            }
        } catch (err) {
            console.error('删除智能体会话失败:', err)
        }
    }

    /** 置顶/取消置顶 */
    async function togglePin(session: SessionVO) {
        try {
            if (session.pinned) {
                await unpinAgentSession(session.id)
            } else {
                await pinAgentSession(session.id)
            }
            loadSessions(true)
        } catch (err) {
            console.error('置顶操作失败:', err)
        }
    }

    /** 手动重命名 */
    async function renameSession(sessionId: number, title: string) {
        try {
            await renameAgentSessionTitle(sessionId, title)
            loadSessions(true)
        } catch (err) {
            console.error('重命名失败:', err)
        }
    }

    /** AI 生成标题 */
    async function aiGenerateTitle(sessionId: number) {
        try {
            await generateAgentSessionTitle(sessionId)
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
