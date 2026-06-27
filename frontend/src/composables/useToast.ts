/**
 * useToast — 全局顶部居中提示
 *
 * 提供 showToast(msg, type?, duration?) 方法。
 * 不同 type 的默认显示时长：
 *   - success: 1 秒
 *   - error:   2.5 秒
 *   - info:    2 秒
 * 配合 ToastNotification.vue 在 App.vue 中使用。
 */
import {reactive} from 'vue'

export type ToastType = 'success' | 'error' | 'info'

interface ToastState {
    visible: boolean
    message: string
    type: ToastType
}

/** 各类型的默认显示时长（毫秒） */
const DURATION: Record<ToastType, number> = {
    success: 1000,
    error: 2500,
    info: 2000,
}

const state = reactive<ToastState>({
    visible: false,
    message: '',
    type: 'success',
})

let timer: ReturnType<typeof setTimeout> | null = null

export function showToast(message: string, type: ToastType = 'success', duration?: number) {
    if (timer) clearTimeout(timer)
    state.message = message
    state.type = type
    state.visible = true
    const ms = duration ?? DURATION[type]
    timer = setTimeout(() => {
        state.visible = false
    }, ms)
}

export function useToast() {
    return {state, showToast}
}
