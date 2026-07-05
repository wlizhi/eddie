/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * useRelativeTime — 相对时间格式化（30s 自动刷新）
 *
 * 来源：ChatSidebar.vue formatTime + 定时器
 */
import {onMounted, onUnmounted, ref} from 'vue'

/** 格式化时间（响应式：now.value 变化时自动重算） */
function formatTime(nowTs: number, dateInput: string | number): string {
    const date = new Date(dateInput)
    const diff = nowTs - date.getTime()
    const minutes = Math.floor(diff / 60000)
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}小时前`
    const days = Math.floor(hours / 24)
    if (days < 7) return `${days}天前`
    return date.toLocaleDateString('zh-CN')
}

export function useRelativeTime() {
    const now = ref(Date.now())
    let timeTimer: ReturnType<typeof setInterval> | null = null

    onMounted(() => {
        timeTimer = setInterval(() => {
            now.value = Date.now()
        }, 30_000)
    })

    onUnmounted(() => {
        if (timeTimer) {
            clearInterval(timeTimer)
            timeTimer = null
        }
    })

    return {
        now,
        formatTime: (dateInput: string | number) => formatTime(now.value, dateInput),
    }
}
