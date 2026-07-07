/**
 * @author Eddie
 * {@code @date} 2026-07-08
 */

import {ref, watch} from 'vue'
import {fetchConfigs} from '@/api/settings'

const GENERAL_SETTINGS_KEY = 'GENERAL_SETTINGS'

/** 开发者模式是否已从后端加载 */
let loaded = false

/**
 * 开发者模式响应式状态
 */
export const developerMode = ref(false)

/**
 * 从后端加载开发者模式配置（仅首次加载有效）
 */
export async function loadDeveloperMode(): Promise<void> {
  if (loaded) return
  loaded = true
  try {
    const configs = await fetchConfigs()
    const raw = configs[GENERAL_SETTINGS_KEY]
    const settings = raw ? JSON.parse(raw) : {}
    if (settings.developerMode != null) {
      developerMode.value = settings.developerMode === true
    }
  } catch {
    // 使用默认值 false
  }
}

/**
 * 同步开发者模式状态到 DOM，方便 CSS 选择器控制调试信息的显隐。
 */
watch(developerMode, (val) => {
  if (val) {
    document.documentElement.dataset.developerMode = 'true'
  } else {
    delete document.documentElement.dataset.developerMode
  }
}, {immediate: true})
