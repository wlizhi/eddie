/**
 * useDragSort — 通用拖拽排序逻辑
 *
 * 来源：ChatSidebar.vue 助手列表拖拽排序
 */
import {ref} from 'vue'

export function useDragSort<T extends { id: number }>(
    list: T[],
    onSort: (ids: number[]) => Promise<void>,
    onError?: () => void
) {
    const dragIndex = ref<number | null>(null)
    const dragOverIndex = ref<number | null>(null)

    function onDragStart(index: number) {
        dragIndex.value = index
    }

    function onDragOver(e: DragEvent, index: number) {
        e.preventDefault()
        dragOverIndex.value = index
    }

    function onDragLeave() {
        dragOverIndex.value = null
    }

    async function onDrop() {
        if (dragIndex.value === null || dragOverIndex.value === null) return
        if (dragIndex.value === dragOverIndex.value) {
            dragIndex.value = null
            dragOverIndex.value = null
            return
        }

        const items = [...list]
        const [moved] = items.splice(dragIndex.value, 1)
        items.splice(dragOverIndex.value, 0, moved)
        list.splice(0, list.length, ...items)

        dragIndex.value = null
        dragOverIndex.value = null

        try {
            await onSort(items.map(a => a.id))
        } catch (err) {
            console.error('排序保存失败:', err)
            onError?.()
        }
    }

    function onDragEnd() {
        dragIndex.value = null
        dragOverIndex.value = null
    }

    return {
        dragIndex,
        dragOverIndex,
        onDragStart,
        onDragOver,
        onDragLeave,
        onDrop,
        onDragEnd,
    }
}
