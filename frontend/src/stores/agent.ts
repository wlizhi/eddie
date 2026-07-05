/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体管理 Store
 *
 * 管理：智能体列表、当前选中、CRUD 操作
 * 结构参照 assistant store，独立管理
 */
import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {AgentCreateRequest, AgentUpdateRequest, AgentVO} from '@/types/agent'
import * as agentApi from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
    // ========== 状态 ==========

    /** 智能体列表 */
    const list = ref<AgentVO[]>([])

    /** 当前选中的智能体 ID */
    const activeId = ref<number | null>(null)

    /** 是否正在加载 */
    const loading = ref(false)

    /** 列表加载完成标记 */
    const loaded = ref(false)

    // ========== 计算属性 ==========

    /** 当前选中的智能体对象 */
    const activeAgent = computed(() =>
        list.value.find(a => a.id === activeId.value) ?? null
    )

    /** 仅启用的智能体列表 */
    const enabledList = computed(() =>
        list.value.filter(a => a.enabled === 1)
    )

    /** 是否有启用的智能体 */
    const hasEnabled = computed(() => enabledList.value.length > 0)

    // ========== 方法 ==========

    /**
     * 加载智能体列表
     * @param showAll true=查询全部, false=仅启用（默认 false）
     * @param force 是否强制重新加载（跳过 loaded 缓存）
     */
    async function loadList(showAll = true, force = true) {
        if (loaded.value && !force) return
        loading.value = true
        try {
            list.value = await agentApi.fetchAgentList(showAll)
            loaded.value = true

            // 首次加载时自动选中第一个启用的智能体
            if (activeId.value === null && enabledList.value.length > 0) {
                activeId.value = enabledList.value[0].id
            }
        } catch (err) {
            console.error('加载智能体列表失败:', err)
        } finally {
            loading.value = false
        }
    }

    /** 选中智能体 */
    function select(id: number) {
        activeId.value = id
    }

    /**
     * 新建智能体
     * 成功后自动选中新创建的智能体
     */
    async function create(data: AgentCreateRequest): Promise<AgentVO | null> {
        try {
            const created = await agentApi.createAgent(data)
            list.value.push(created)
            activeId.value = created.id
            return created
        } catch (err) {
            console.error('新建智能体失败:', err)
            return null
        }
    }

    /**
     * 更新智能体
     */
    async function update(id: number, data: AgentUpdateRequest): Promise<AgentVO | null> {
        try {
            const updated = await agentApi.updateAgent(id, data)
            const idx = list.value.findIndex(a => a.id === id)
            if (idx !== -1) {
                list.value[idx] = updated
            }
            return updated
        } catch (err) {
            console.error('更新智能体失败:', err)
            return null
        }
    }

    /**
     * 删除智能体
     * 如果删除的是当前选中的智能体，自动切换到下一个启用的智能体
     */
    async function remove(id: number): Promise<boolean> {
        try {
            await agentApi.deleteAgent(id)
            list.value = list.value.filter(a => a.id !== id)
            if (activeId.value === id) {
                activeId.value = enabledList.value[0]?.id ?? null
            }
            return true
        } catch (err) {
            console.error('删除智能体失败:', err)
            return false
        }
    }

    return {
        // 状态
        list,
        activeId,
        loading,
        loaded,
        // 计算属性
        activeAgent,
        enabledList,
        hasEnabled,
        // 方法
        loadList,
        select,
        create,
        update,
        remove,
    }
})
