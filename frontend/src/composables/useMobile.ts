/**
 * @author Eddie
 * @date 2026-06-28
 */

/**
 * useMobile — 移动端断点检测 & visualViewport 键盘高度
 *
 * 用法：
 *   import { useMobile } from '@/composables/useMobile'
 *   const { isMobile, keyboardHeight } = useMobile()
 *
 * isMobile:      视口宽度 < 768px 时为 true
 * keyboardHeight: iOS Safari 键盘弹起时的高度差值（px），桌面端恒为 0
 */
import {onMounted, onUnmounted, ref} from 'vue'

const MOBILE_BREAKPOINT = 768

export function useMobile() {
    const isMobile = ref(false)
    const keyboardHeight = ref(0)

    function checkWidth() {
        isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
    }

    function onVisualViewportResize() {
        if (window.visualViewport) {
            keyboardHeight.value = Math.max(0, window.innerHeight - window.visualViewport.height)
        }
    }

    function onResize() {
        checkWidth()
        onVisualViewportResize()
    }

    onMounted(() => {
        checkWidth()
        onVisualViewportResize()
        window.addEventListener('resize', onResize)
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', onVisualViewportResize)
        }
    })

    onUnmounted(() => {
        window.removeEventListener('resize', onResize)
        if (window.visualViewport) {
            window.visualViewport.removeEventListener('resize', onVisualViewportResize)
        }
    })

    return {isMobile, keyboardHeight}
}
