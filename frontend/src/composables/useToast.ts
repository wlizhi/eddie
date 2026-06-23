/**
 * useToast — 全局顶部居中提示
 *
 * 提供 showToast(msg, type?) 方法，1 秒后自动消失。
 * 配合 ToastNotification.vue 在 App.vue 中使用。
 */
import {reactive} from 'vue'

export type ToastType = 'success' | 'error' | 'info'

interface ToastState {
    visible: boolean
    message: string
    type: ToastType
}

const state = reactive<ToastState>({
    visible: false,
    message: '',
    type: 'success',
})

let timer: ReturnType<typeof setTimeout> | null = null

export function showToast(message: string, type: ToastType = 'success') {
    if (timer) clearTimeout(timer)
    state.message = message
    state.type = type
    state.visible = true
    timer = setTimeout(() => {
        state.visible = false
    }, 1000)
}

export function useToast() {
    return {state, showToast}
}
