<!--
  MessageList.vue — 消息列表

  功能：
  - 渲染所有聊天消息（用户 + 助手）
  - 助手的思考过程（thinking）折叠/展开
  - 助手消息的元数据显示（时间、耗时、token 用量）
  - 流式响应时自动滚动到底部

  数据来源：直接从 Pinia store (useChatStore) 读取

  与父组件通信：
  - qaMode (boolean) — 问答模式下用户消息左对齐
-->
<script setup lang="ts">
import {nextTick, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {Bot, ChevronDown} from '@lucide/vue'
import {renderMd} from '@/utils/markdown'
import {formatTime} from '@/utils/format'

defineProps<{
  qaMode: boolean
}>()

const chatStore = useChatStore()

/** 消息容器 DOM，用于自动滚动 */
const messageListRef = ref<HTMLElement | null>(null)

/** 跟踪每条消息的 thinking 展开状态 */
const thinkingExpanded = ref<Record<string, boolean>>({})

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

/** 切换 thinking 折叠状态 */
function toggleThinking(msgId: string) {
  thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
}
</script>

<template>
  <div ref="messageListRef" class="message-list" :class="{ 'qa-mode': qaMode }">
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
          <button class="thinking-toggle" @click="toggleThinking(msg.id)">
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
</template>

<style scoped>
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

/* 问答模式下用户消息也左对齐 */
.message-list.qa-mode .user-row {
  align-self: flex-start;
  flex-direction: row;
}

.message-list.qa-mode .user-bubble {
  border-bottom-right-radius: 10px;
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
</style>
