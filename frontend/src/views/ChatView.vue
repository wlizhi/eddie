<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {Bot, ChevronDown, FileText, Maximize2, MessageSquare, Minimize2, Send, Square} from '@lucide/vue'
import {NSelect} from 'naive-ui'
import {Marked} from 'marked'

const marked = new Marked({breaks: true, gfm: true})

/** Markdown → HTML 渲染 */
function renderMd(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text) as string
  } catch {
    return text
  }
}

const chatStore = useChatStore()

/** 输入框文本 */
const inputText = ref('')
/** textarea DOM 引用 */
const inputRef = ref<HTMLTextAreaElement | null>(null)
/** 消息容器 DOM，用于自动滚动 */
const messageListRef = ref<HTMLElement | null>(null)
/** 跟踪每条消息的 thinking 展开状态 */
const thinkingExpanded = ref<Record<string, boolean>>({})
/** IME 输入法组合状态（如拼音输入法选词中） */
const isComposing = ref(false)

/** 宽屏模式：true=全宽，false=窄居中 */
const isWideMode = ref(false)
/** 聊天模式：true=左右交替，false=问答模式左对齐 */
const isChatMode = ref(true)

onMounted(() => {
  chatStore.loadModels()
})

/** 自动滚动到底部 */
async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 消息列表变化时自动滚动
watch(
    () => chatStore.messages.length,
    () => scrollToBottom(),
)
// 流式内容变化时自动滚动
watch(
    () => chatStore.currentAnswer,
    () => scrollToBottom(),
)

/** 发送消息 */
function handleSend() {
  const text = inputText.value
  if (!text.trim() || chatStore.isStreaming) return
  inputText.value = ''
  chatStore.sendMessage(text)
  // 新消息默认展开 thinking
  nextTick(() => {
    const msgs = chatStore.messages
    const last = msgs[msgs.length - 1]
    if (last && last.role === 'assistant') {
      thinkingExpanded.value[last.id] = true
    }
    // 保持焦点在输入框
    inputRef.value?.focus()
  })
}

/** 中断流式请求 */
function handleAbort() {
  chatStore.abortStream()
}

/** 切换 thinking 折叠状态 */
function toggleThinking(msgId: string) {
  thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
}

/** 处理输入框键盘事件：Enter 发送，Shift+Enter 换行 */
function handleKeydown(e: KeyboardEvent) {
  // IME 输入法组合中（如拼音选词），忽略 Enter 按键
  if (e.isComposing || e.keyCode === 229) return
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

/** IME 组合开始 */
function onCompositionStart() {
  isComposing.value = true
}

/** IME 组合结束 */
function onCompositionEnd() {
  isComposing.value = false
}

/** textarea 自适应高度 */
function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

/** 按供应商分组 — naive-ui NSelect 分组格式（用于下拉"更多"） */
const groupedOptions = chatStore.modelSelectors.map((selector) => ({
  type: 'group' as const,
  label: selector.providerName,
  key: selector.providerCode,
  children: selector.models.map((m) => ({
    label: m.displayName ?? m.modelId,
    value: m.modelId,
  })),
}))

/** 当前选中供应商的名称（用于底部栏标签显示） */
const currentProviderName = computed(() => {
  const sel = chatStore.modelSelectors.find(
      s => s.providerCode === chatStore.currentProviderCode,
  )
  return sel?.providerName ?? ''
})

/** 当前供应商下的模型列表（快速切换胶囊） */
const currentProviderModels = computed(() => {
  const sel = chatStore.modelSelectors.find(
      s => s.providerCode === chatStore.currentProviderCode,
  )
  return sel?.models ?? []
})

/** 前 3 个模型作为胶囊按钮，其余进入 "更多" 下拉 */
const QUICK_MODEL_LIMIT = 3

/** 快速切换胶囊模型（前 N 个） */
const quickModels = computed(() => {
  return currentProviderModels.value.slice(0, QUICK_MODEL_LIMIT)
})

/** 是否有更多模型需要下拉选择 */
const hasMoreModels = computed(() => {
  return currentProviderModels.value.length > QUICK_MODEL_LIMIT
})

/** NSelect "更多" 下拉选中时同步 providerCode */
function onModelSelect(modelId: string) {
  for (const sel of chatStore.modelSelectors) {
    const found = sel.models.find(m => m.modelId === modelId)
    if (found) {
      chatStore.selectModel(found.modelId, found.providerCode)
      return
    }
  }
}

/** 格式化日期时间 */
function formatTime(ts: number | undefined | null): string {
  if (ts == null || ts <= 0) return ''
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<template>
  <div class="chat-view" :class="{ narrow: !isWideMode, 'qa-mode': !isChatMode }">
    <!-- ===== 右上角悬浮工具栏 ===== -->
    <div class="view-toolbar">
      <button
          class="toolbar-btn"
          :title="isWideMode ? '切换窄屏' : '切换全宽'"
          @click="isWideMode = !isWideMode"
      >
        <Maximize2 v-if="isWideMode" :size="14" :stroke-width="1.8"/>
        <Minimize2 v-else :size="14" :stroke-width="1.8"/>
      </button>
      <button
          class="toolbar-btn"
          :title="isChatMode ? '切换问答模式' : '切换聊天模式'"
          @click="isChatMode = !isChatMode"
      >
        <MessageSquare v-if="isChatMode" :size="14" :stroke-width="1.8"/>
        <FileText v-else :size="14" :stroke-width="1.8"/>
      </button>
    </div>

    <!-- ===== 消息列表 ===== -->
    <div v-if="chatStore.hasMessages" ref="messageListRef" class="message-list">
      <div
          v-for="msg in chatStore.messages"
          :key="msg.id"
          class="message-row"
          :class="[msg.role === 'user' ? 'user-row' : 'assistant-row']"
      >
        <!-- 头像 -->
        <div class="avatar-col">
          <div v-if="msg.role === 'user'" class="avatar user-avatar">
            <span class="avatar-text">我</span>
          </div>
          <div v-else class="avatar assistant-avatar">
            <Bot :size="16" :stroke-width="1.8"/>
          </div>
        </div>

        <!-- 消息内容 -->
        <div class="msg-col">
          <!-- thinking 内容（仅 assistant） -->
          <div v-if="msg.role === 'assistant' && msg.thinking" class="thinking-section">
            <button
                class="thinking-toggle"
                @click="toggleThinking(msg.id)"
            >
              <ChevronDown
                  :size="13"
                  :stroke-width="2"
                  class="chevron"
                  :class="{ rotated: thinkingExpanded[msg.id] }"
              />
              <span>思考过程</span>
            </button>
            <div v-if="thinkingExpanded[msg.id]" class="thinking-content" v-html="renderMd(msg.thinking)"/>
          </div>

          <!-- 消息正文 -->
          <div class="message-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
            <div
                v-if="msg.content"
                class="message-content markdown-body"
                v-html="renderMd(msg.content)"
            ></div>
            <div
                v-else-if="chatStore.isStreaming && msg === chatStore.messages[chatStore.messages.length - 1] && msg.role === 'assistant'"
                class="message-content"
            >思考中...
            </div>
          </div>

          <!-- 元数据（仅 assistant） -->
          <div v-if="msg.role === 'assistant' && msg.metadata" class="metadata">
            <template v-if="msg.metadata.timestamp">
              <span class="meta-time">{{ formatTime(msg.metadata.timestamp) }}</span>
              <span class="meta-divider">|</span>
            </template>
            <span v-if="msg.metadata.durationMs != null" class="meta-duration">
              耗时 {{ (msg.metadata.durationMs / 1000).toFixed(1) }}s
            </span>
            <span class="meta-divider">|</span>
            <span v-if="msg.metadata.totalTokens != null" class="meta-tokens">
              {{ msg.metadata.totalTokens }} tokens
              <span v-if="msg.metadata.promptTokens != null || msg.metadata.completionTokens != null"
                    class="meta-tokens-detail">
                (输入 {{ msg.metadata.promptTokens ?? '?' }} · 输出 {{ msg.metadata.completionTokens ?? '?' }})
              </span>
            </span>
            <span v-else-if="msg.metadata.promptTokens != null || msg.metadata.completionTokens != null"
                  class="meta-tokens">
              输入 {{ msg.metadata.promptTokens ?? '?' }} · 输出 {{ msg.metadata.completionTokens ?? '?' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 空状态 ===== -->
    <div v-else class="empty-state">
      <div class="empty-icon">💬</div>
      <h2>开始新对话</h2>
      <p class="empty-hint">选择模型，输入你的问题，开始与 AI 助手交流</p>
      <div class="suggestions">
        <div class="suggestion-card" @click="inputText = '帮我写一段 Python 代码'">帮我写一段 Python 代码</div>
        <div class="suggestion-card" @click="inputText = '解释一下什么是 RESTful API'">解释一下什么是 RESTful API</div>
        <div class="suggestion-card" @click="inputText = 'Vue 3 和 React 有什么区别'">Vue 3 和 React 有什么区别</div>
        <div class="suggestion-card" @click="inputText = '翻译一段英文文本'">翻译一段英文文本</div>
      </div>
    </div>

    <!-- ===== 底部输入区域 ===== -->
    <div class="input-area">
      <!-- 输入框 + 按钮 -->
      <div class="input-row">
        <textarea
            ref="inputRef"
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息..."
            rows="1"
            @keydown="handleKeydown"
            @compositionstart="onCompositionStart"
            @compositionend="onCompositionEnd"
            @input="autoResize"
        />
        <button
            v-if="!chatStore.isStreaming"
            class="send-btn"
            :disabled="!inputText.trim()"
            title="发送"
            @click="handleSend"
        >
          <Send :size="16" :stroke-width="2"/>
        </button>
        <button
            v-else
            class="stop-btn"
            title="中断"
            @click="handleAbort"
        >
          <Square :size="14" :stroke-width="2"/>
        </button>
      </div>

      <!-- 底部栏：模型选择器（胶囊按钮 + 下拉） + 功能开关 -->
      <div class="bottom-bar">
        <!-- 模型选择区 -->
        <div class="model-selector-area">
          <!-- 供应商标签 -->
          <span v-if="currentProviderName" class="provider-tag">{{ currentProviderName }}</span>

          <!-- 胶囊按钮：快速切换当前供应商下的前几个模型 -->
          <button
              v-for="m in quickModels"
              :key="m.modelId"
              class="model-chip"
              :class="{ active: m.modelId === chatStore.currentModelId }"
              @click="chatStore.selectModel(m.modelId, m.providerCode)"
          >
            {{ m.displayName ?? m.modelId }}
          </button>

          <!-- 更多模型下拉 -->
          <NSelect
              v-if="hasMoreModels"
              :value="chatStore.currentModelId"
              :options="groupedOptions"
              size="tiny"
              class="more-select"
              placeholder="更多 ▾"
              :show-arrow="false"
              @update:value="onModelSelect"
          />
        </div>

        <!-- 功能开关区（预留） -->
        <div class="feature-toggles">
          <button class="toggle-chip disabled" title="联网搜索（即将上线）">
            🌐 联网
          </button>
          <button class="toggle-chip disabled" title="深度思考（即将上线）">
            💡 深度思考
          </button>
        </div>

        <span v-if="chatStore.isStreaming" class="streaming-hint">正在生成...</span>
      </div>
    </div>
  </div>
</template>

<style>
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
  'Noto Sans SC', sans-serif;
  background: #ffffff;
  color: #1f1f1f;
}
</style>

<style scoped>
/* ===== 布局 ===== */
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ===== 右上角悬浮工具栏 ===== */
.view-toolbar {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 50;
  display: flex;
  gap: 2px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  opacity: 0.65;
  transition: opacity 0.2s, background 0.2s, box-shadow 0.2s;
}

.chat-view:hover .view-toolbar,
.view-toolbar:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.toolbar-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: background 0.15s, color 0.15s, transform 0.12s;
}

.toolbar-btn:hover {
  background: #e8eaee;
  color: #374151;
}

.toolbar-btn:active {
  transform: scale(0.92);
}

.chat-view {
  position: relative;
}

/* ===== 窄模式：内容居中缩窄 ===== */
.chat-view.narrow .message-list {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.chat-view.narrow .input-area {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

/* ===== 问答模式：全部左对齐 ===== */
.chat-view.qa-mode .user-row {
  align-self: flex-start;
  flex-direction: row;
}

.chat-view.qa-mode .user-bubble {
  border-bottom-right-radius: 10px;
}

/* ===== 消息列表 ===== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-row {
  display: flex;
  gap: 8px;
  max-width: 800px;
}

.user-row {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.assistant-row {
  align-self: flex-start;
}

/* ===== 头像 ===== */
.avatar-col {
  flex-shrink: 0;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.user-avatar {
  background: #2563eb;
  color: #fff;
}

.assistant-avatar {
  background: #f0f1f3;
  color: #6b7280;
}

.avatar-text {
  font-size: 10px;
}

/* ===== 消息列 ===== */
.msg-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

/* ===== 气泡 ===== */
.message-bubble {
  padding: 6px 10px;
  border-radius: 10px;
  line-height: 1.55;
  font-size: 13.5px;
}

.user-bubble {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 3px;
}

.assistant-bubble {
  background: #f4f5f7;
  color: #1f1f1f;
  border-bottom-left-radius: 3px;
}

.message-content {
  word-break: break-word;
}

/* ===== 思考区域 ===== */
.thinking-section {
  border-left: 2px solid #d1d5db;
  padding-left: 8px;
  margin-bottom: 3px;
}

.thinking-toggle {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 4px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  color: #9ca3af;
  transition: background 0.15s, color 0.15s;
}

.thinking-toggle:hover {
  background: #f0f1f3;
  color: #6b7280;
}

.chevron {
  transition: transform 0.2s;
}

.chevron.rotated {
  transform: rotate(0deg);
}

.chevron:not(.rotated) {
  transform: rotate(-90deg);
}

.thinking-content {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 4px 0;
}

/* ===== 元数据 ===== */
.metadata {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #9ca3af;
  padding: 2px 2px 0;
  flex-wrap: wrap;
  user-select: none;
}

.meta-time {
  font-variant-numeric: tabular-nums;
}

.meta-divider {
  color: #d1d5db;
  font-size: 8px;
}

.meta-duration {
  color: #9ca3af;
}

.meta-tokens {
  color: #9ca3af;
}

.meta-tokens-detail {
  color: #b0b7c3;
  font-size: 9px;
}

/* ===== 空状态 ===== */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

h2 {
  font-size: 24px;
  font-weight: 600;
}

.empty-hint {
  color: #888;
  font-size: 14px;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 24px;
  max-width: 400px;
}

.suggestion-card {
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  text-align: center;
  transition: background 0.15s;
}

.suggestion-card:hover {
  background: #f5f5f5;
}

/* ===== 输入区域 ===== */
.input-area {
  flex-shrink: 0;
  border-top: 1px solid #e6e8ec;
  padding: 10px 16px 12px;
  background: #fff;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e0e2e6;
  border-radius: 8px;
  font-size: 13.5px;
  font-family: inherit;
  line-height: 1.5;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s;
  color: #1f1f1f;
  background: #fafbfc;
}

.chat-input:focus {
  border-color: #2563eb;
  background: #fff;
}

.send-btn,
.stop-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  flex-shrink: 0;
}

.send-btn {
  background: #2563eb;
  color: #fff;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: #1d4ed8;
}

.stop-btn {
  background: #ef4444;
  color: #fff;
}

.stop-btn:hover {
  background: #dc2626;
}

/* ===== 底部栏 ===== */
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding: 0 2px;
  gap: 8px;
}

/* 模型选择区（左侧） */
.model-selector-area {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* 供应商标签 */
.provider-tag {
  font-size: 11px;
  font-weight: 500;
  color: #9ca3af;
  background: #f0f1f3;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  user-select: none;
}

/* 模型胶囊按钮 */
.model-chip {
  padding: 3px 10px;
  font-size: 12px;
  font-family: inherit;
  line-height: 1.4;
  border: 1px solid #e0e2e6;
  border-radius: 14px;
  background: #fafbfc;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.model-chip:hover {
  background: #f0f1f3;
  border-color: #c4c7cc;
}

.model-chip.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

/* "更多" 下拉选择器 */
.more-select {
  min-width: 70px;
  max-width: 110px;
}

/* 功能开关区（右侧） */
.feature-toggles {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.toggle-chip {
  padding: 3px 10px;
  font-size: 12px;
  font-family: inherit;
  line-height: 1.4;
  border: 1px solid #e0e2e6;
  border-radius: 14px;
  background: #fafbfc;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.toggle-chip:hover {
  background: #f0f1f3;
  border-color: #c4c7cc;
}

.toggle-chip.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-chip.disabled:hover {
  background: #fafbfc;
  border-color: #e0e2e6;
}

.streaming-hint {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
}
</style>

<style>
.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin: .6em 0 .3em;
  font-weight: 600;
  line-height: 1.4
}

.markdown-body h1 {
  font-size: 1.3em
}

.markdown-body h2 {
  font-size: 1.15em
}

.markdown-body h3 {
  font-size: 1.05em
}

.markdown-body p {
  margin: .4em 0;
  line-height: 1.7
}

.markdown-body ul, .markdown-body ol {
  padding-left: 1.5em;
  margin: .4em 0
}

.markdown-body li {
  margin: .2em 0
}

.markdown-body code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: .88em;
  padding: .15em .4em;
  border-radius: 4px;
  background: rgba(0, 0, 0, .06)
}

.markdown-body pre {
  margin: .6em 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #1e1e1e;
  overflow-x: auto
}

.markdown-body pre code {
  padding: 0;
  background: 0 0;
  color: #d4d4d4;
  font-size: .82em;
  line-height: 1.6
}

.markdown-body blockquote {
  border-left: 3px solid #d1d5db;
  padding: .3em 0 .3em 12px;
  margin: .4em 0;
  color: #6b7280
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: .6em 0;
  font-size: .92em
}

.markdown-body td, .markdown-body th {
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  text-align: left
}

.markdown-body th {
  background: #f0f1f3;
  font-weight: 600
}

.markdown-body hr {
  border: 0;
  border-top: 1px solid #e0e2e6;
  margin: .8em 0
}

.markdown-body a {
  color: #2563eb;
  text-decoration: underline
}

.markdown-body strong {
  font-weight: 600
}

.markdown-body em {
  font-style: italic
}
</style>
