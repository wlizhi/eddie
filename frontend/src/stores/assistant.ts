/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * 助手管理 Store
 *
 * 管理：助手列表、当前选中、CRUD 操作
 * 与 chat store 解耦，各自独立
 */
import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {AssistantCreateRequest, AssistantUpdateRequest, AssistantVO} from '@/types/assistant'
import * as assistantApi from '@/api/assistant'

export const useAssistantStore = defineStore('assistant', () => {
    // ========== 状态 ==========

    /** 助手列表 */
    const list = ref<AssistantVO[]>([])

    /** 当前选中的助手 ID */
    const activeId = ref<number | null>(null)

    /** 是否正在加载 */
    const loading = ref(false)

    /** 列表加载完成标记 */
    const loaded = ref(false)

    // ========== 计算属性 ==========

    /** 当前选中的助手对象 */
    const activeAssistant = computed(() =>
        list.value.find(a => a.id === activeId.value) ?? null
    )

    /** 仅启用的助手列表 */
    const enabledList = computed(() =>
        list.value.filter(a => a.enabled === 1)
    )

    /** 是否有启用的助手 */
    const hasEnabled = computed(() => enabledList.value.length > 0)

    // ========== 方法 ==========

    /**
     * 加载助手列表
     * @param showAll true=查询全部, false=仅启用（默认 false）
     * @param force 是否强制重新加载（跳过 loaded 缓存）
     */
    async function loadList(showAll = true, force = true) {
        if (loaded.value && !force) return
        loading.value = true
        try {
            list.value = await assistantApi.fetchAssistantList(showAll)
            loaded.value = true

            // 首次加载时自动选中第一个启用的助手
            if (activeId.value === null && enabledList.value.length > 0) {
                activeId.value = enabledList.value[0].id
            }
        } catch (err) {
            console.error('加载助手列表失败:', err)
        } finally {
            loading.value = false
        }
    }

    /** 选中助手 */
    function select(id: number) {
        activeId.value = id
    }

    /**
     * 新建助手
     * 成功后自动选中新创建的助手
     */
    async function create(data: AssistantCreateRequest): Promise<AssistantVO | null> {
        try {
            const created = await assistantApi.createAssistant(data)
            list.value.push(created)
            activeId.value = created.id
            return created
        } catch (err) {
            console.error('新建助手失败:', err)
            return null
        }
    }

    /**
     * 更新助手
     */
    async function update(id: number, data: AssistantUpdateRequest): Promise<AssistantVO | null> {
        try {
            const updated = await assistantApi.updateAssistant(id, data)
            const idx = list.value.findIndex(a => a.id === id)
            if (idx !== -1) {
                list.value[idx] = updated
            }
            return updated
        } catch (err) {
            console.error('更新助手失败:', err)
            return null
        }
    }

    /**
     * 删除助手
     * 如果删除的是当前选中的助手，自动切换到下一个启用的助手
     */
    async function remove(id: number): Promise<boolean> {
        try {
            await assistantApi.deleteAssistant(id)
            list.value = list.value.filter(a => a.id !== id)
            if (activeId.value === id) {
                activeId.value = enabledList.value[0]?.id ?? null
            }
            return true
        } catch (err) {
            console.error('删除助手失败:', err)
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
        activeAssistant,
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
