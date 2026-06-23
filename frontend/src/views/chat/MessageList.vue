<!--
  MessageList.vue — 消息列表

  功能：
  - 渲染所有聊天消息（用户 + 助手）
  - 助手的思考过程（thinking）折叠/展开
  - 助手消息的元数据显示（时间、耗时、token 用量）
  - 流式响应时自动滚动到底部
  - 滚动到顶部时自动加载更早的消息（游标分页）

  数据来源：直接从 Pinia store (useChatStore) 读取

  与父组件通信：
  - qaMode (boolean) — 问答模式下用户消息左对齐
-->
<script setup lang="ts">
import {nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {useAssistantStore} from '@/stores/assistant'
import {ChevronDown, Copy, Loader} from '@lucide/vue'
import {renderMd} from '@/utils/markdown'
import {formatTime} from '@/utils/format'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'

defineProps<{
  qaMode: boolean
}>()

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 消息容器 DOM，用于自动滚动 */
const messageListRef = ref<HTMLElement | null>(null)

/** 跟踪每条消息的 thinking 展开状态 */
const thinkingExpanded = ref<Record<string, boolean>>({})

/** 记录加载更多前的高度，用于保持滚动位置 */
let prevScrollHeight = 0

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
// 切换会话时自动滚动到底部（处理消息数量相同时 length watcher 不触发的情况）
watch(
    () => chatStore.currentConversationId,
    () => scrollToBottom(),
)

// 组件首次挂载时滚动到底部（v-if 条件渲染下，挂载时 messages 已有值，watcher 不会触发）
onMounted(() => {
  scrollToBottom()
})

/** 切换 thinking 折叠状态 */
function toggleThinking(msgId: string) {
  thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
}

/** 当前正在显示"已复制"提示的消息 ID */
const copiedMessageId = ref<string | null>(null)

/** 复制消息内容到剪贴板（去除 SSE 协议引入的首部换行） */
function copyContent(msgId: string, content: string) {
  navigator.clipboard.writeText(content.replace(/^\n+/, '')).then(() => {
    copiedMessageId.value = msgId
    setTimeout(() => {
      if (copiedMessageId.value === msgId) {
        copiedMessageId.value = null
      }
    }, 2000)
  })
}

/** 币种代码 → 符号映射 */
const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$', CNY: '¥', EUR: '€', GBP: '£', JPY: '¥', KRW: '₩', HKD: 'HK$', TWD: 'NT$', SGD: 'S$',
}

function currencySymbol(currency?: string | null): string {
  if (!currency) return '$'
  return CURRENCY_SYMBOLS[currency] ?? currency + ' '
}

/**
 * 滚动事件处理
 *
 * 当用户滚动到接近顶部时（阈值 50px），触发加载更早的消息。
 * 加载完成后保持滚动位置，避免页面跳动。
 */
function onScroll() {
  const el = messageListRef.value
  if (!el) return

  // 接近顶部时触发加载
  if (el.scrollTop <= 50 && chatStore.hasMoreMessages && !chatStore.isLoadingMore) {
    prevScrollHeight = el.scrollHeight
    chatStore.loadMoreMessages().then(() => {
      // 加载完成后保持滚动位置（补偿新增内容的高度）
      nextTick(() => {
        if (messageListRef.value) {
          messageListRef.value.scrollTop = messageListRef.value.scrollHeight - prevScrollHeight
        }
      })
    })
  }
}
</script>

<template>
  <div ref="messageListRef" class="message-list" :class="{ 'qa-mode': qaMode }" @scroll="onScroll">
    <!-- 顶部加载指示器 -->
    <div v-if="chatStore.isLoadingMore" class="load-more-indicator">
      <Loader :size="16" class="spinner"/>
      <span>加载更早的消息...</span>
    </div>
    <div v-else-if="!chatStore.hasMoreMessages && chatStore.messages.length > 0" class="no-more-hint">
      已加载全部消息
    </div>

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
          <AssistantAvatar
              v-if="assistantStore.activeAssistant"
              :name="assistantStore.activeAssistant.name"
              :avatar="assistantStore.activeAssistant.avatar"
              :size="28"
          />
          <span v-else class="avatar-text">AI</span>
        </div>
      </div>

      <!-- 消息内容 -->
      <div class="msg-col">
        <!-- 助手名称 -->
        <div v-if="msg.role === 'assistant' && assistantStore.activeAssistant" class="assistant-name-label">
          {{ assistantStore.activeAssistant.name }}
        </div>

        <!-- thinking 内容（仅 assistant） -->
        <div
            v-if="msg.role === 'assistant' && (msg.thinking || (chatStore.isStreaming && msg === chatStore.messages[chatStore.messages.length - 1] && !msg.content))"
            class="thinking-section">
          <button class="thinking-toggle" @click="toggleThinking(msg.id)">
            <ChevronDown
                :size="13"
                :stroke-width="2"
                class="chevron"
                :class="{ rotated: thinkingExpanded[msg.id] }"
            />
            <span v-if="msg.content || !chatStore.isStreaming">思考过程</span>
            <span v-else class="thinking-pending">
              <span class="thinking-text">思考中<span
                  class="dots-blink"><span>.</span><span>.</span><span>.</span></span></span>
            </span>
          </button>
          <div v-if="thinkingExpanded[msg.id] && msg.thinking" class="thinking-content"
               v-html="renderMd(msg.thinking)"/>
        </div>

        <!-- 消息正文 -->
        <div class="message-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
          <div
              v-if="msg.content"
              class="message-content markdown-body"
              v-html="renderMd(msg.content)"
          ></div>
          <!-- 复制按钮 -->
          <button
              v-if="msg.content"
              class="copy-btn"
              :class="msg.role === 'user' ? 'copy-btn-user' : 'copy-btn-assistant'"
              :data-copied="copiedMessageId === msg.id || undefined"
              @click="copyContent(msg.id, msg.content)"
              :title="copiedMessageId === msg.id ? '已复制' : '复制消息'"
          >
            <Copy v-if="copiedMessageId !== msg.id" :size="13" :stroke-width="2"/>
            <span v-else class="copied-text">已复制</span>
          </button>
        </div>

        <!-- 元数据（仅 assistant） -->
        <div v-if="msg.role === 'assistant' && msg.metadata" class="metadata">
          <template v-if="msg.metadata.timestamp">
            <span class="meta-time">{{ formatTime(msg.metadata.timestamp) }}</span>
          </template>
          <template v-if="msg.metadata.durationMs != null">
            <span v-if="msg.metadata.timestamp" class="meta-divider">|</span>
            <span class="meta-duration">
              耗时 {{ (msg.metadata.durationMs / 1000).toFixed(1) }}s
            </span>
          </template>
          <template v-if="msg.metadata.totalTokens != null">
            <span v-if="msg.metadata.timestamp || msg.metadata.durationMs != null" class="meta-divider">|</span>
            <span class="meta-tokens">
              {{ msg.metadata.totalTokens }} tokens
              <span v-if="msg.metadata.promptTokens != null || msg.metadata.completionTokens != null"
                    class="meta-tokens-detail">
                (输入 {{ msg.metadata.promptTokens ?? '?' }} · 输出 {{ msg.metadata.completionTokens ?? '?' }})
              </span>
            </span>
          </template>
          <template v-else-if="msg.metadata.promptTokens != null || msg.metadata.completionTokens != null">
            <span v-if="msg.metadata.timestamp || msg.metadata.durationMs != null" class="meta-divider">|</span>
            <span class="meta-tokens">
              输入 {{ msg.metadata.promptTokens ?? '?' }} · 输出 {{ msg.metadata.completionTokens ?? '?' }}
            </span>
          </template>
          <!-- 预估费用 -->
          <template v-if="msg.metadata.costEstimate != null">
            <span
                v-if="msg.metadata.timestamp || msg.metadata.durationMs != null || msg.metadata.totalTokens != null || msg.metadata.promptTokens != null || msg.metadata.completionTokens != null"
                class="meta-divider">|</span>
            <span class="meta-cost">≈{{ currencySymbol(msg.metadata.currency) }}{{
                msg.metadata.costEstimate.toFixed(6)
              }}</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style src="./message-list.css" scoped/>
