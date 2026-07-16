<!--
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手弹窗根组件
 * 通过 IPC 获取选中文本和配置，按 action 动态渲染对应子组件
 -->

<template>
  <div class="popup-root" :style="rootStyle">
    <n-config-provider :theme-overrides="naiveThemeOverrides">
      <div class="header">
        <h2 class="header-title" v-html="headerTitleHtml"></h2>
        <div class="header-actions">
          <button class="icon-btn" id="pinBtn" :title="isPinned ? '取消置顶' : '置顶'" @click="togglePin">
            <Pin :size="fontSizeNum" :stroke-width="1.5" :fill="isPinned ? 'currentColor' : 'none'" :class="{ pinned: isPinned }" />
          </button>
          <button class="icon-btn" id="closeBtn" title="关闭" @click="closePopup">
            <X :size="fontSizeNum" :stroke-width="1.5" />
          </button>
        </div>
      </div>
      <component :is="currentComponent" :data="popupData" />
    </n-config-provider>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, onMounted, onUnmounted, watch} from 'vue'
import {Pin, X} from '@lucide/vue'
import {NConfigProvider} from 'naive-ui'
import TranslatePopup from './TranslatePopup.vue'
import ExplainPopup from './ExplainPopup.vue'
import SummarizePopup from './SummarizePopup.vue'
import OpenPopup from './OpenPopup.vue'
import CopyPopup from './CopyPopup.vue'
import {findTheme} from '@/assets/themes/index'
import {generateAccentVariants, findColorScheme} from '@/composables/useDisplaySettings'

/** 弹窗主题：键为完整 CSS 变量名（如 --bg-primary），值为颜色值 */
type PopupTheme = Record<string, string>

interface PopupData {
  action: string
  text: string
  fontSize: number
  theme: PopupTheme
  targetLang?: string
}

/** 弹窗内 CSS 实际引用的变量键名列表（用于 computePopupTheme 兜底） */
const POPUP_VAR_KEYS = [
  '--bg-primary', '--bg-secondary', '--bg-tertiary', '--bg-hover',
  '--text-primary', '--text-secondary', '--text-tertiary', '--text-quaternary', '--text-muted',
  '--border-base', '--border-focus',
  '--msg-assistant-bg', '--msg-assistant-text',
  '--accent-default', '--accent-hover',
  '--danger-default', '--success-default',
  '--text-code-bg', '--text-code',
  '--hljs-color', '--hljs-keyword', '--hljs-string', '--hljs-number',
  '--hljs-comment', '--hljs-type', '--hljs-built-in', '--hljs-punctuation',
  '--hljs-variable', '--hljs-tag', '--hljs-selector-class', '--hljs-title',
  '--hljs-regexp', '--hljs-meta', '--hljs-deletion', '--hljs-addition',
  '--scrollbar-thumb',
]

const popupData = ref<PopupData>({
  action: 'open',
  text: '',
  fontSize: 14,
  theme: {},
})

/**
 * 划词助手完整配置（弹窗内自行控制内部样式/行为）
 */
const selectionConfig = ref<SelectionAssistantConfig | null>(null)

/** 当前弹窗是否处于置顶状态 */
const isPinned = ref(false)

/** 当前字体大小数值（用于图标 size prop，与根 font-size 保持一致） */
const fontSizeNum = computed(() => popupData.value.fontSize || 14)

/** 当前字体 CSS font-family 字符串，由 displaySettings.fontFamily 驱动 */
const fontFamilyCss = ref("-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif")

// 主题更新取消订阅函数（组件卸载时清理）
let unsubscribeTheme: (() => void) | undefined

// 配置变更取消订阅函数（组件卸载时清理）
let unsubscribeSettings: (() => void) | undefined

// 置顶状态变更取消订阅函数（组件卸载时清理）
let unsubscribePin: (() => void) | undefined

/**
 * 从主题定义标识符计算弹窗所需的全部 CSS 变量
 * 以 findTheme('default') 的对应 mode 变量集为基础，再覆盖指定主题和强调色
 */
function computePopupTheme(
  themeId: string,
  themeMode: 'light' | 'dark',
  colorScheme: string,
): PopupTheme {
  // 以 default 主题的对应 mode 为基底（确保所有 POPUP_VAR_KEYS 都有值）
  const base = findTheme('default')?.variables?.[themeMode] || {}
  const result: PopupTheme = {}
  for (const key of POPUP_VAR_KEYS) {
    result[key] = base[key] || ''
  }

  // 覆盖指定主题的变量（含代码高亮）
  const definition = findTheme(themeId)
  const vars = definition?.variables?.[themeMode]
  if (vars) {
    for (const [key, val] of Object.entries(vars)) {
      result[key] = val
    }
  }

  // 覆盖强调色变体
  const scheme = findColorScheme(colorScheme)
  if (scheme) {
    const sv = themeMode === 'dark' ? scheme.dark : scheme.light
    result['--accent-default'] = sv.accent
    result['--accent-hover'] = sv.hover
    result['--accent-light-bg'] = sv.lightBg
    result['--accent-light-border'] = sv.lightBorder
    result['--accent-ring'] = sv.ring
    result['--border-focus'] = sv.borderFocus
    result['--text-accent'] = sv.textAccent
  } else if (/^#/.test(colorScheme)) {
    const gen = generateAccentVariants(colorScheme, themeMode === 'dark')
    result['--accent-default'] = gen.accent
    result['--accent-hover'] = gen.hover
    result['--accent-light-bg'] = gen.lightBg
    result['--accent-light-border'] = gen.lightBorder
    result['--accent-ring'] = gen.ring
    result['--border-focus'] = gen.borderFocus
    result['--text-accent'] = gen.textAccent
  }

  return result
}

const HEADER_ICONS: Record<string, string> = {
  translate: '<svg width="1em" height="1em" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M2.5 5h9"/><path d="M2.5 9h9"/><path d="M7 1.5a7 7 0 0 1 0 11"/><path d="M7 1.5a7 7 0 0 0 0 11"/></svg>',
  explain: '<svg width="1em" height="1em" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M2 2.5v9a1 1 0 0 0 1 1h3.5L7 11l.5 1.5H11a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1z"/><path d="M7 11V4"/></svg>',
  summarize: '<svg width="1em" height="1em" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="3.5" x2="11.5" y2="3.5"/><line x1="5" y1="7" x2="11.5" y2="7"/><line x1="5" y1="10.5" x2="11.5" y2="10.5"/><circle cx="2.5" cy="3.5" r=".8"/><circle cx="2.5" cy="7" r=".8"/><circle cx="2.5" cy="10.5" r=".8"/></svg>',
  copy: '<svg width="1em" height="1em" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><rect x="4.5" y="4.5" width="7" height="7" rx=".8"/><path d="M2.5 10.5v-7a1 1 0 0 1 1-1h7"/></svg>',
}

const HEADER_TITLES: Record<string, string> = {
  translate: '翻译',
  explain: '解释',
  summarize: '总结',
  copy: '复制',
  open: '美化',
}

const currentComponent = computed(() => {
  const map: Record<string, any> = {
    translate: TranslatePopup,
    explain: ExplainPopup,
    summarize: SummarizePopup,
    copy: CopyPopup,
    open: OpenPopup,
  }
  return map[popupData.value.action] || OpenPopup
})

const headerTitleHtml = computed(() => {
  const icon = HEADER_ICONS[popupData.value.action] || ''
  const title = HEADER_TITLES[popupData.value.action] || 'AI 处理结果'
  return `${icon} ${title}`
})

/**
 * rootStyle 直接注入全部原始 CSS 变量名
 * 弹窗内任何地方使用 var(--bg-primary)、var(--msg-assistant-bg) 等均可正确取值
 */
const rootStyle = computed(() => {
  const t = popupData.value.theme
  const fs = popupData.value.fontSize || 14
  // 按项目统一比例计算字号变量，与 chat 消息一致
    const bodySize = Math.round(fs * 0.9) + 'px'     // --font-size-body: 0.9em
    const baseSize = Math.round(fs * 0.867) + 'px'   // --font-size-base: 0.867em
    const smallSize = Math.round(fs * 0.8) + 'px'    // --font-size-small: 0.8em
    const controlHeight = Math.round(Math.round(fs * 0.8) * 2) + 'px' // 控件统一高度（与 heightTiny 一致）
    return {
      fontFamily: fontFamilyCss.value,
      fontSize: fs + 'px',
      '--font-size-body': bodySize,
      '--font-size-base': baseSize,
      '--font-size-small': smallSize,
      '--control-height': controlHeight,
    background: t['--bg-primary'],
    color: t['--text-primary'],
    ...t,
  }
})

// 同步字体 CSS 变量到 document.body，确保 teleport 出去的 NSelect 下拉菜单也能读取
watch(() => popupData.value.fontSize, (fs) => {
  fs = fs || 14
  document.body.style.setProperty('--font-size-body', Math.round(fs * 0.9) + 'px')
  document.body.style.setProperty('--font-size-base', Math.round(fs * 0.867) + 'px')
  document.body.style.setProperty('--font-size-small', Math.round(fs * 0.8) + 'px')
}, {immediate: true})

/** Naive UI 主题覆盖：从弹窗 CSS 变量映射（与 useNaiveThemeOverrides 一致） */
const naiveThemeOverrides = computed(() => {
  const t = popupData.value.theme
  const fs = popupData.value.fontSize || 14
  const basePx = Math.round(fs * 0.867)
  const smallPx = Math.round(fs * 0.8)

  const accentDefault = t['--accent-default'] || '#6366f1'
  const accentHover = t['--accent-hover'] || '#4f46e5'
  const accentRing = t['--accent-ring'] || 'rgba(99,102,241,0.3)'
  const borderFocusVal = t['--border-focus'] || accentDefault
  const bgPrimary = t['--bg-primary'] || '#ffffff'
  const bgSecondary = t['--bg-secondary'] || '#f4f4f5'
  const bgTertiary = t['--bg-tertiary'] || '#e4e4e7'
  const bgHover = t['--bg-hover'] || '#e4e4e7'
  const textPrimary = t['--text-primary'] || '#18181b'
  const textTertiary = t['--text-tertiary'] || '#a1a1aa'
  const borderBase = t['--border-base'] || '#e4e4e7'

  return {
    common: {
      primaryColor: accentDefault,
      primaryColorHover: accentHover,
      primaryColorPressed: accentHover,
      bodyColor: bgPrimary,
      textColor1: textPrimary,
      textColor2: t['--text-secondary'] || '#71717a',
      textColor3: textTertiary,
      borderColor: borderBase,
      hoverColor: bgHover,
      inputColor: bgTertiary,
      popoverColor: bgSecondary,
      placeholderColor: textTertiary,
      borderRadius: '6px',
    },
    Select: {
      menuColor: bgPrimary,
      color: bgTertiary,
      border: `1px solid ${borderBase}`,
      borderFocus: `1px solid ${borderFocusVal}`,
      boxShadowFocus: `0 0 0 2px ${accentRing}`,
      placeholderColor: textTertiary,
      actionTextColor: accentDefault,
      fontSizeTiny: smallPx + 'px',
      fontSizeSmall: basePx + 'px',
      fontSizeMedium: basePx + 'px',
      optionFontSizeSmall: basePx + 'px',
      optionFontSizeMedium: basePx + 'px',
      optionHeightSmall: Math.round(basePx * 2) + 'px',
      optionPaddingSmall: `${Math.round(basePx * 0.3)}px ${Math.round(basePx * 0.8)}px`,
      arrowSize: smallPx + 'px',
    },
  }
})

function togglePin() {
  window.selectionAPI?.togglePin()
}

function closePopup() {
  window.selectionAPI?.closePopup()
}

async function loadSelectionConfig() {
  try {
    const config = await window.selectionAPI?.getSelectionConfig()
    if (config) {
      selectionConfig.value = config
    }
  } catch (err) {
    console.error('[Popup] Failed to load config:', err)
  }
}

onMounted(async () => {
  // 必须在任何 await 之前同步注册监听器，否则 ready-to-show 触发时 IPC 消息会丢失
  unsubscribePin = window.selectionAPI?.onPinChanged?.((pinned: boolean) => {
    console.log('[Popup] Pin state changed:', pinned)
    isPinned.value = pinned
  })

  try {
    const data = await window.selectionAPI?.getPopupData()
    if (data) {
      // 仅提取 action/text/targetLang，theme 用 computePopupTheme 重新计算以获取完整 CSS 变量
      popupData.value = {
        ...popupData.value,
        action: data.action,
        text: data.text,
        targetLang: data.targetLang,
        fontSize: data.fontSize || popupData.value.fontSize,
      }
    }
  } catch (err) {
    console.error('[Popup] Failed to get popup data:', err)
  }

  // 通过 displaySettings 重新计算完整 CSS 变量主题 + 字体类型
  try {
    const settings = await window.selectionAPI?.getDisplaySettings()
    if (settings) {
      fontFamilyCss.value = settings.fontFamily || fontFamilyCss.value
      popupData.value.theme = computePopupTheme(
        settings.themeId,
        settings.themeMode as 'light' | 'dark',
        settings.colorScheme,
      )
    }
  } catch (err) {
    console.error('[Popup] Failed to get display settings:', err)
  }

  // 获取划词助手完整配置（弹窗自行控制内部样式/行为）
  await loadSelectionConfig()

  // 监听主进程推送的主题变更通知 → 弹窗本地计算 CSS 变量
  unsubscribeTheme = window.selectionAPI?.onThemeChanged?.(async () => {
    console.log('[Popup] Theme changed, refreshing...')
    try {
      const settings = await window.selectionAPI?.getDisplaySettings()
      if (settings) {
        fontFamilyCss.value = settings.fontFamily || fontFamilyCss.value
        popupData.value = {
          ...popupData.value,
          theme: computePopupTheme(settings.themeId, settings.themeMode as 'light' | 'dark', settings.colorScheme),
          fontSize: settings.fontSize || popupData.value.fontSize,
        }
      }
    } catch (err) {
      console.error('[Popup] Failed to refresh theme:', err)
    }
  })

  // 监听配置变更通知 → 重新加载配置
  unsubscribeSettings = window.selectionAPI?.onSettingsChanged?.(async () => {
    console.log('[Popup] Settings changed, reloading config...')
    await loadSelectionConfig()
  })
})

onUnmounted(() => {
  if (unsubscribeTheme) {
    unsubscribeTheme()
  }
  if (unsubscribeSettings) {
    unsubscribeSettings()
  }
  if (unsubscribePin) {
    unsubscribePin()
  }
})
</script>

<style>
@import '@/assets/styles/markdown.css';

/* ===== 自定义滚动条（与主进程前端 theme.css 一致） ===== */
* {
    scrollbar-width: thin;
    scrollbar-color: var(--scrollbar-thumb) transparent;
}

*::-webkit-scrollbar {
    width: 5px;
    height: 5px;
}

*::-webkit-scrollbar-thumb {
    background-color: var(--scrollbar-thumb);
    border-radius: 3px;
}

*::-webkit-scrollbar-thumb:hover {
    background-color: color-mix(in srgb, var(--scrollbar-thumb) 85%, black);
}

*::-webkit-scrollbar-track {
    background: transparent;
}

/* ===== 全局重置 ===== */
*{margin:0;padding:0;box-sizing:border-box}
html,body{height:100%;overflow:hidden}
body{display:flex;flex-direction:column}

/* ===== 根容器 ===== */
.popup-root{
  display:flex;flex-direction:column;height:100vh;overflow:hidden;
}

/* Naive UI ConfigProvider 包装层：参与 flex 链条 */
.popup-root > .n-config-provider{
  flex:1;min-height:0;display:flex;flex-direction:column;
}

/* ===== 通用 Header ===== */
.header{
  display:flex;align-items:center;justify-content:space-between;
  padding:6px 12px;
  border-bottom:1px solid var(--border-base);
  background:var(--bg-secondary);
  -webkit-app-region:drag;flex-shrink:0;
}
.header-title{
  font-size:var(--font-size-base);font-weight:600;display:flex;align-items:center;gap:6px;
}
.header-title svg{flex-shrink:0}
.header-actions{display:flex;gap:3px;-webkit-app-region:no-drag}

.icon-btn{
  display:inline-flex;align-items:center;justify-content:center;
  width:22px;height:22px;border:none;border-radius:4px;
  cursor:pointer;background:transparent;
  color:var(--text-tertiary);transition:background .15s;
}
.icon-btn:hover{background:var(--bg-hover);color:var(--text-primary)}
.icon-btn .pinned{color:var(--accent-default)}

/* ===== 内容区通用 ===== */
.content{
  flex:1;display:flex;flex-direction:column;
  padding:8px 12px;gap:4px;min-height:0;overflow-y:auto;
}

/* ===== 折叠原文区 ===== */
.collapse-section{
  border:1px solid var(--border-base);border-radius:6px;
  overflow:hidden;flex-shrink:0;
}
.collapse-header{
  display:flex;align-items:center;gap:6px;
  padding:8px 12px;cursor:pointer;user-select:none;
  background:var(--bg-tertiary);transition:background .12s;
}
.collapse-header:hover{background:var(--bg-hover)}
.collapse-icon{
  font-size:var(--font-size-small);color:var(--text-tertiary);
  transition:transform .15s;flex-shrink:0;
}
.collapse-icon.rotated{transform:rotate(90deg)}
.collapse-label{
  font-size:var(--font-size-small);font-weight:500;color:var(--text-tertiary);
  text-transform:uppercase;letter-spacing:.5px;
}
.collapse-body{
  border-top:1px solid var(--border-base);padding:10px 12px;
  background:var(--bg-primary);
}
.collapse-body .sel-text{
  padding:0;border:none;background:transparent;
  font-size:var(--font-size-body);line-height:1.6;color:var(--text-secondary);
  max-height:120px;overflow-y:auto;white-space:pre-wrap;word-break:break-all;
}

/* ===== 流式结果区 ===== */
.result-section{
  flex:1;display:flex;flex-direction:column;gap:4px;min-height:0;
}
.result-label{
  font-size:var(--font-size-small);font-weight:500;color:var(--text-tertiary);
  display:flex;align-items:center;gap:4px;
  text-transform:uppercase;letter-spacing:.5px;flex-shrink:0;
}
.result-content{
  flex:1;padding:8px 10px;border-radius:6px;
  background:var(--msg-assistant-bg);border:1px solid var(--border-base);
  font-size:var(--font-size-body);line-height:1.6;color:var(--text-primary);
  min-height:0;overflow-y:auto;
}

/* ===== loading 动画 ===== */
.loading-indicator{
  display:flex;align-items:center;gap:4px;padding:6px 0;
}
.loading-dot{
  width:6px;height:6px;border-radius:50%;
  background:var(--text-tertiary);animation:loadingBounce 1.2s ease-in-out infinite;
}
.loading-dot:nth-child(2){animation-delay:.2s}
.loading-dot:nth-child(3){animation-delay:.4s}
@keyframes loadingBounce{
  0%,80%,100%{opacity:.3;transform:scale(.8)}
  40%{opacity:1;transform:scale(1)}
}

/* ===== 错误提示 ===== */
.error-msg{color:var(--danger-default);font-size:var(--font-size-body);line-height:1.6;}

/* ===== 成功提示（copy） ===== */
.success-msg{
  display:flex;flex-direction:column;align-items:center;justify-content:center;
  gap:10px;padding:24px 0;color:var(--text-secondary);
}
.success-msg .check-icon{color:var(--success-default)}
.success-msg .msg-text{font-size:var(--font-size-body);font-weight:500}

/* ===== Naive UI Select 字体跟随全局（对齐 theme.css）=====
   弹窗为独立页面，不加载全局 theme.css，需单独设置。
   覆盖 Naive UI 内部固定 px 字号，使用 var() 使字体跟随 rootStyle 动态变化。 */
.n-base-selection .n-base-selection-input,
.n-base-selection .n-base-selection-placeholder,
.n-base-selection .n-base-selection-label,
.n-base-selection .n-base-selection-tags {
    font-size: var(--font-size-base) !important;
}
.n-base-select-menu .n-base-select-option {
    font-size: var(--font-size-base) !important;
}
.n-base-select-menu .n-base-select-option--group-header {
    font-size: var(--font-size-small) !important;
    font-weight: 600;
}
.n-dropdown-option-body {
    font-size: var(--font-size-base) !important;
}

</style>
