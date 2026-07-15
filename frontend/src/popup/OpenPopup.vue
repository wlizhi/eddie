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
        <span class="collapse-icon" :class="{rotated: !collapsed}">&#9654;</span>
        <span class="collapse-label">原文</span>
      </div>
      <div v-show="!collapsed" class="collapse-body">
        <div class="sel-text">{{ data.text }}</div>
      </div>
    </div>
    <div class="result-section">
      <div class="result-label">
        <svg width="1em" height="1em" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 1.5h5l3.5 3.5v7.5a1 1 0 0 1-1 1h-7.5a1 1 0 0 1-1-1v-10a1 1 0 0 1 1-1z"/><path d="M8 1.5v3.5h3.5"/>
        </svg>
        AI 处理结果
      </div>
      <div class="result-content">
        <div v-if="loading" class="loading-indicator">
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
        </div>
        <div v-else-if="error" class="error-msg">{{ error }}</div>
        <div v-else class="markdown-body" v-html="rendered"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {renderMd} from '@/utils/markdown'

const props = defineProps<{ data: { text: string } }>()

const collapsed = ref(true)
const result = ref('')
const loading = ref(true)
const error = ref('')
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

onMounted(() => startStream())
</script>
