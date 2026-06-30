/**
 * @author Eddie
 * @date 2026-06-22
 */

/**
 * useVirtualList — 固定高度虚拟滚动
 *
 * 仅渲染可视区域 + 缓冲区的列表项，避免大量 DOM 节点导致卡顿。
 * 配合无限滚动使用时，在距底部 threshold 像素时触发 onLoadMore。
 *
 * 使用方式：在模板中给滚动容器绑定 @scroll="onScroll"
 */
import {computed, type Ref, ref, watch} from 'vue'

export interface VirtualListOptions<T> {
    /** 数据源（全量） */
    items: Ref<T[]>
    /** 单项高度（px），必须固定 */
    itemHeight: number
    /** 可视区域上下额外渲染的项数（防白屏） */
    buffer?: number
    /** 触底加载阈值（px），距底部多少时触发 */
    loadMoreThreshold?: number
    /** 触底回调 */
    onLoadMore?: () => void
}

export function useVirtualList<T>(
    containerRef: Ref<HTMLElement | null>,
    options: VirtualListOptions<T>
) {
    const {items, itemHeight, buffer = 5, loadMoreThreshold = 200, onLoadMore} = options

    const scrollTop = ref(0)
    const containerHeight = ref(0)

    /** 总高度 */
    const totalHeight = computed(() => items.value.length * itemHeight)

    /** 起始索引 */
    const startIndex = computed(() => Math.max(0, Math.floor(scrollTop.value / itemHeight) - buffer))

    /** 结束索引（不含） */
    const endIndex = computed(() => {
        const end = Math.ceil((scrollTop.value + containerHeight.value) / itemHeight) + buffer
        return Math.min(items.value.length, end)
    })

    /** 可见项 */
    const visibleItems = computed(() => items.value.slice(startIndex.value, endIndex.value))

    /** 可见区域偏移量 */
    const offsetY = computed(() => startIndex.value * itemHeight)

    /** 节流：防止高频 onLoadMore 回调 */
    let loadMoreTimer: ReturnType<typeof setTimeout> | null = null

    /** 滚动处理 —— 模板中 @scroll="onScroll" 绑定 */
    function onScroll(e: Event) {
        const el = e.target as HTMLElement
        scrollTop.value = el.scrollTop

        // 首次或 resize 时更新容器高度
        if (containerHeight.value === 0 || containerHeight.value !== el.clientHeight) {
            containerHeight.value = el.clientHeight
        }

        // 触底加载（节流 200ms）
        if (onLoadMore && !loadMoreTimer) {
            const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < loadMoreThreshold
            if (nearBottom) {
                loadMoreTimer = setTimeout(() => {
                    loadMoreTimer = null
                    onLoadMore()
                }, 200)
            }
        }
    }

    /** 手动更新容器高度 */
    function updateHeight() {
        if (containerRef.value) {
            containerHeight.value = containerRef.value.clientHeight
        }
    }

    // items 变化时重新计算（确保数据更新后高度正确）
    watch(items, () => {
        setTimeout(updateHeight, 0)
    })

    return {
        visibleItems,
        offsetY,
        totalHeight,
        onScroll,
        updateHeight,
    }
}
