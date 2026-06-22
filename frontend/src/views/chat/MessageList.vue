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
import {useAssistantStore} from '@/stores/assistant'
import {ChevronDown} from '@lucide/vue'
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

<style src="./message-list.css" scoped/>
