<!--
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 翻译弹窗组件 — 折叠原文 + 流式翻译结果
-->

<template>
  <div class="content">
    <div class="lang-bar">
      <div class="source-badge">自动检测</div>
      <svg class="arrow-icon" :class="{ pulsing: streaming }" width="1em" height="1em" viewBox="0 0 14 14" fill="none"
           stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
        <line x1="2" y1="7" x2="12" y2="7"/>
        <polyline points="9 4 12 7 9 10"/>
      </svg>
      <n-select
          v-model:value="selectedLang"
          :options="langOptions"
          size="tiny"
          class="lang-select"
          @update:value="onLangChange"
      />
      <button class="icon-btn refresh-btn" title="重新生成" @click="retry" :disabled="streaming">
        <RefreshCw stroke-width="1.3" :class="{ spinning: streaming }"/>
      </button>
    </div>
    <div class="collapse-section">
      <div class="collapse-header" @click="collapsed = !collapsed">
        <n-tooltip trigger="hover" placement="top" :show-arrow="false">
          <template #trigger>
            <span class="collapse-icon" :class="{rotated: !collapsed}">&#9654;</span>
          </template>
          原文
        </n-tooltip>
      </div>
      <div v-show="!collapsed" class="collapse-body">
        <div class="sel-text">{{ data.text }}</div>
      </div>
    </div>
    <div class="result-section">
      <div class="result-content" ref="resultRef">
        <button class="copy-btn-float" :class="{copied: copyDone, visible: showCopy}" :title="copyDone ? '已复制' : '复制内容'" @click.stop="copyContent" @mouseenter="showCopy = true" @mouseleave="showCopy = false">
          <Copy :size="12" :stroke-width="1.5" />
        </button>
        <div v-if="loading" class="loading-indicator">
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
        </div>
        <div v-else-if="error" class="error-msg">{{ error }}</div>
        <div v-else class="markdown-body" v-html="rendered" @mouseenter="showCopy = true" @mouseleave="showCopy = false"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {renderMd} from '@/utils/markdown'
import {RefreshCw, Copy} from '@lucide/vue'
import {NSelect, NTooltip} from 'naive-ui'

const props = defineProps<{ data: { text: string; targetLang?: string } }>()

const collapsed = ref(true)
const result = ref('')
const loading = ref(true)
const streaming = ref(false)  // 流式传输中（控制箭头动画，持续到 metadata）
const error = ref('')
const copyDone = ref(false)
const showCopy = ref(false)
const rendered = computed(() => renderMd(result.value))
const resultRef = ref<HTMLElement | null>(null)

const langOptions = [
  {value: 'zh-CN', label: '简体中文', zhLabel: '简体中文'},
  {value: 'zh-TW', label: '繁體中文', zhLabel: '繁体中文'},
  {value: 'en', label: 'English', zhLabel: '英文'},
  {value: 'ja', label: '日本語', zhLabel: '日文'},
  {value: 'ko', label: '한국어', zhLabel: '韩文'},
  {value: 'fr', label: 'Français', zhLabel: '法文'},
  {value: 'de', label: 'Deutsch', zhLabel: '德文'},
  {value: 'es', label: 'Español', zhLabel: '西班牙文'},
  {value: 'pt', label: 'Português', zhLabel: '葡萄牙文'},
  {value: 'ru', label: 'Русский', zhLabel: '俄文'},
  {value: 'it', label: 'Italiano', zhLabel: '意大利文'},
  {value: 'ar', label: 'العربية', zhLabel: '阿拉伯文'},
  {value: 'nl', label: 'Nederlands', zhLabel: '荷兰文'},
  {value: 'pl', label: 'Polski', zhLabel: '波兰文'},
  {value: 'tr', label: 'Türkçe', zhLabel: '土耳其文'},
  {value: 'vi', label: 'Tiếng Việt', zhLabel: '越南文'},
  {value: 'th', label: 'ไทย', zhLabel: '泰文'},
  {value: 'hi', label: 'हिन्दी', zhLabel: '印地文'},
  {value: 'id', label: 'Bahasa Indonesia', zhLabel: '印尼文'},
  {value: 'ms', label: 'Bahasa Melayu', zhLabel: '马来文'},
  {value: 'sv', label: 'Svenska', zhLabel: '瑞典文'},
  {value: 'da', label: 'Dansk', zhLabel: '丹麦文'},
  {value: 'fi', label: 'Suomi', zhLabel: '芬兰文'},
  {value: 'no', label: 'Norsk', zhLabel: '挪威文'},
  {value: 'cs', label: 'Čeština', zhLabel: '捷克文'},
  {value: 'hu', label: 'Magyar', zhLabel: '匈牙利文'},
  {value: 'ro', label: 'Română', zhLabel: '罗马尼亚文'},
  {value: 'uk', label: 'Українська', zhLabel: '乌克兰文'},
  {value: 'el', label: 'Ελληνικά', zhLabel: '希腊文'},
  {value: 'he', label: 'עברית', zhLabel: '希伯来文'},
]

const selectedLang = ref(props.data.targetLang || 'zh-CN')

/** 根据选中代码获取中文描述（供后端 prompt 使用） */
const targetLangDisplay = computed(() => {
  const opt = langOptions.find(o => o.value === selectedLang.value)
  return opt ? opt.zhLabel : '简体中文'
})

function onLangChange() {
  retry()
}

/** 当前请求的 AbortController，用于取消上一次未完成的请求 */
let currentAbort: AbortController | null = null

/**
 * 发起流式翻译请求
 */
async function startStream() {
  streaming.value = true
  // 取消上一次未完成的请求
  if (currentAbort) {
    currentAbort.abort()
  }
  currentAbort = new AbortController()
  const {signal} = currentAbort
  const trimmed = props.data.text?.trim()
  if (!trimmed) {
    error.value = '选中文本为空，无法处理'
    loading.value = false
    streaming.value = false
    return
  }
  try {
    const response = await fetch('/api/selection-assistant/stream', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        action: 'translate',
        text: props.data.text,
        targetLang: targetLangDisplay.value,
      }),
      signal,
    })

    if (!response.ok || !response.body) {
      error.value = `请求失败: ${response.status}`
      loading.value = false
      streaming.value = false
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const {done, value} = await reader.read()
      if (done) break

      buffer += decoder.decode(value, {stream: true})
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const block of parts) {
        if (!block.trim()) continue
        processEvent(block)
      }
    }

    // 处理剩余 buffer
    if (buffer.trim()) {
      processEvent(buffer)
    }

    loading.value = false
    streaming.value = false
  } catch (err: any) {
    error.value = `网络请求失败: ${err.message}`
    loading.value = false
    streaming.value = false
  }
}

function processEvent(block: string) {
  const lines = block.split('\n')
  let eventType = ''
  let dataStr = ''

  for (const line of lines) {
    if (line.startsWith('event:')) eventType = line.slice(6).trim()
    else if (line.startsWith('data:')) dataStr = line.slice(5).trim()
  }

  if (!dataStr) return

  try {
    const parsed = JSON.parse(dataStr)

    switch (eventType) {
      case 'delta': {
        if (parsed.content) {
          loading.value = false
          result.value += parsed.content
        }
        break
      }
      case 'metadata':
        loading.value = false
        streaming.value = false
        break
      case 'error': {
        error.value = parsed.message || '未知错误'
        loading.value = false
        streaming.value = false
        break
      }
    }
  } catch {
    // ignore parse errors
  }
}

/**
 * 重新生成翻译
 */
function retry() {
  result.value = ''
  error.value = ''
  loading.value = true
  streaming.value = true
  startStream()
}

/** 复制渲染后的文本内容到剪贴板 */
async function copyContent() {
  const text = result.value || props.data.text
  try {
    await navigator.clipboard.writeText(text)
    copyDone.value = true
    setTimeout(() => { copyDone.value = false }, 1500)
  } catch { /* ignore */ }
}

onMounted(() => {
  startStream()
})
</script>

<style scoped>
/* ===== 语言选择栏 ===== */
.lang-bar {
  display: flex;
  align-items: center;
  gap: .4em;
  padding: .4em 0;
  flex-shrink: 0;
}

.source-badge {
  font-size: var(--font-size-small);
  color: var(--text-secondary);
  padding: 0 .5em;
  border: 1px solid var(--border-base);
  border-radius: 4px;
  background: var(--bg-tertiary);
  white-space: nowrap;
  user-select: none;
  display: flex;
  align-items: center;
}

.arrow-icon {
  color: var(--text-tertiary);
  flex-shrink: 0;
  transition: color .2s
}

.arrow-icon.pulsing {
  color: var(--accent-default);
  animation: arrowPulse .8s ease-in-out infinite
}

@keyframes arrowPulse {
  0%, 100% {
    opacity: .6;
    transform: translateX(0)
  }
  50% {
    opacity: 1;
    transform: translateX(2px)
  }
}

.lang-select {
  font-size: var(--font-size-small) !important;
  flex: none;
  min-width: 8rem;
  width: 12rem;
  display: flex;
  align-items: center;
}

/* NSelect tiny 尺寸：与 .source-badge 对齐 */
.lang-select :deep(.n-base-selection) {
  min-height: auto !important;
  padding: 0 !important;
  background: var(--bg-tertiary) !important;
  border-radius: 4px !important
}

.lang-select :deep(.n-base-selection-label) {
  font-size: var(--font-size-small) !important;
  color: var(--text-secondary) !important;
  min-height: auto !important;
  height: auto !important;
}

.refresh-btn {
  flex-shrink: 0;
  padding: 0 !important;
  margin: 0;
  font-size: inherit;
  line-height: 1;
  width: 1em !important;
  height: 1em !important;
  display: inline-flex;
  align-items: center;
  justify-content: center
}

.refresh-btn svg {
  width: 1em;
  height: 1em;
  display: block
}

.refresh-btn:disabled {
  cursor: default;
  opacity: .5
}

.refresh-btn:disabled .spinning {
  animation: spin 2s linear infinite
}

@keyframes spin {
  to {
    transform: rotate(360deg)
  }
}

/* ===== 悬浮复制按钮 ===== */
.copy-btn-float{
  position:absolute;top:6px;right:6px;z-index:2;
  display:inline-flex;align-items:center;justify-content:center;
  width:1.6em;height:1.6em;border:none;border-radius:4px;
  cursor:pointer;background:var(--bg-primary);opacity:0;
  color:var(--text-tertiary);transition:opacity .15s,color .15s,background .15s;
  padding:0;flex-shrink:0;
}
.copy-btn-float.visible{opacity:.8}
.copy-btn-float:hover{opacity:1;background:var(--bg-hover);color:var(--text-primary)}
.copy-btn-float.copied{color:var(--success-default)}
</style>
