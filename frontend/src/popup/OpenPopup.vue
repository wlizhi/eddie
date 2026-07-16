<!--
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 打开/美化弹窗组件 — 折叠原文 + 流式 AI 美化结果
-->

<template>
  <div class="content">
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
      <div class="result-content">
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
import {Copy} from '@lucide/vue'
import {NTooltip} from 'naive-ui'

const props = defineProps<{ data: { text: string } }>()

const collapsed = ref(true)
const result = ref('')
const loading = ref(true)
const error = ref('')
const copyDone = ref(false)
const showCopy = ref(false)
const rendered = computed(() => renderMd(result.value))

async function startStream() {
  const trimmed = props.data.text?.trim()
  if (!trimmed) {
    error.value = '选中文本为空，无法处理'
    loading.value = false
    return
  }
  try {
    const response = await fetch('/api/selection-assistant/stream', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({action: 'beautify', text: props.data.text}),
    })
    if (!response.ok || !response.body) { error.value = `请求失败: ${response.status}`; loading.value = false; return }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const {done, value} = await reader.read()
      if (done) break
      buffer += decoder.decode(value, {stream: true})
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const block of parts) { if (block.trim()) processEvent(block) }
    }
    if (buffer.trim()) processEvent(buffer)
    loading.value = false
  } catch (err: any) {
    error.value = `网络请求失败: ${err.message}`
    loading.value = false
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
      case 'delta': if (parsed.content) { loading.value = false; result.value += parsed.content } break
      case 'metadata': loading.value = false; break
      case 'error': error.value = parsed.message || '未知错误'; loading.value = false; break
    }
  } catch { /* ignore */ }
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

onMounted(() => startStream())
</script>

<style scoped>
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
