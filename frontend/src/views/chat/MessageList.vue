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
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {useAssistantStore} from '@/stores/assistant'
import {Brain, ChevronDown, Copy, Eye, Loader, Pen, RefreshCw} from '@lucide/vue'
import {renderMd} from '@/utils/markdown'
import {formatShortTime} from '@/utils/format'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'
import {displaySettings, getEffectiveFontSize} from '@/composables/useDisplaySettings'

defineProps<{
  qaMode: boolean
}>()

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 工具内部名 → 友好显示名（通用映射，新增工具无需修改） */
function displayToolName(toolName: string): string {
  // 去掉 built_in_ 前缀，下划线转空格，首字母大写
  return toolName
      .replace(/^built_in_/, '')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase())
}

/** 构建工具调用内容的 Markdown：参数（JSON 代码块）+ 正文，中间隔空行 */
function buildToolContent(args: string | undefined, result: string | undefined): string {
  let content = ''
  if (args) {
    try {
      const parsed = JSON.parse(args)
      content += '```json\n' + JSON.stringify(parsed, null, 2) + '\n```'
    } catch {
      content += '```\n' + args + '\n```'
    }
  }
  if (result) {
    // 代码块闭合后，用标准 markdown 段落分隔（两个换行）添加空白行
    if (content) content += '\n\n'
    content += fixNewlines(result)
  }
  console.log('=== buildToolContent output ===', JSON.stringify(content))
  return content
}

/**
 * 修复内容中可能存在的转义换行符 → 真实换行符
 * 某些外部工具返回的文本中，\n 以字面量形式存在而非真实换行符，
 * 需要转换以便 Markdown 渲染器正确解析。
 */
function fixNewlines(text: string): string {
  return text.replace(/\\n/g, '\n')
}

/** 消息容器 DOM，用于自动滚动 */
const messageListRef = ref<HTMLElement | null>(null)

/** 跟踪每条消息的 thinking 展开状态 */
const thinkingExpanded = ref<Record<string, boolean>>({})

/** 跟踪每次工具调用的结果展开状态 */
const toolResultExpanded = ref<Record<string, boolean>>({})

/** 记录加载更多前的高度，用于保持滚动位置 */
let prevScrollHeight = 0

/** 随字体大小动态变化的头像大小（基准字号 × 2.5，与 --avatar-size 保持一致） */
const avatarSize = computed(() => Math.round(getEffectiveFontSize() * 2.2))

/** 用户是否已手动上滑（打断自动滚动） */
const userScrolledAway = ref(false)

/** 判断滚动容器是否在底部附近（100px 阈值） */
function isNearBottom(): boolean {
  const el = messageListRef.value
  if (!el) return true
  return el.scrollTop + el.clientHeight >= el.scrollHeight - 100
}

/** 强制滚动到底部（发送新消息时无条件调用，同时恢复自动滚动） */
async function scrollToBottom() {
  userScrolledAway.value = false
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

/** 条件滚动：仅当用户没有手动上滑时才滚动 */
async function scrollToBottomIfNeeded() {
  if (userScrolledAway.value) return
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 消息列表变化 → 判断：用户发送新消息（isStreaming 变为 true）则强制滚动，否则条件滚动
watch(
    () => chatStore.messages.length,
    (_newLen, _oldLen) => {
      if (chatStore.isStreaming) {
        // 正在流式响应中 → 用户发送了新消息，无条件滚动到底部 + 恢复自动滚动
        scrollToBottom()
        return
      }
      scrollToBottomIfNeeded()
    },
)
// 流式内容变化时条件滚动
watch(
    () => chatStore.currentAnswer,
    () => scrollToBottomIfNeeded(),
)
// 切换会话时强制滚动到底部
watch(
    () => chatStore.currentConversationId,
    () => scrollToBottom(),
)
// metadata 到达时条件滚动
watch(
    () => chatStore.currentMetadata,
    () => scrollToBottomIfNeeded(),
)

// 组件首次挂载时滚动到底部（v-if 条件渲染下，挂载时 messages 已有值，watcher 不会触发）
onMounted(() => {
  scrollToBottom()
})

/** 切换 thinking 折叠状态 */
function toggleThinking(msgId: string) {
  thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
}

/** 切换工具执行结果折叠状态 */
function toggleToolResult(toolIndex: number) {
  const key = String(toolIndex)
  toolResultExpanded.value[key] = !toolResultExpanded.value[key]
}

/** 当前正在显示"已复制"提示的消息 ID */
const copiedMessageId = ref<string | null>(null)

/** 跟踪每条消息的 metadata 展开状态（移动端折叠详情） */
const metaExpanded = ref<Record<string, boolean>>({})

/** 切换 metadata 展开/折叠 */
function toggleMetaExpanded(msgId: string) {
  metaExpanded.value[msgId] = !metaExpanded.value[msgId]
}

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

  // 检测用户是否手动上滑（远离底部），打断自动滚动
  if (!isNearBottom()) {
    userScrolledAway.value = true
  }

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
          <AssistantAvatar
              :name="displaySettings.nickname || '我'"
              :avatar="displaySettings.avatar"
              :size="avatarSize"
          />
        </div>
        <div v-else class="avatar assistant-avatar">
          <AssistantAvatar
              v-if="assistantStore.activeAssistant"
              :name="assistantStore.activeAssistant.name"
              :avatar="assistantStore.activeAssistant.avatar"
              :size="avatarSize"
          />
          <span v-else class="avatar-text">AI</span>
        </div>
      </div>

      <!-- 消息内容 -->
      <div class="msg-col">
        <!-- 用户名称 -->
        <div v-if="msg.role === 'user' && displaySettings.nickname" class="user-name-label">
          {{ displaySettings.nickname }}
        </div>
        <!-- 助手名称 -->
        <div v-if="msg.role === 'assistant' && assistantStore.activeAssistant" class="assistant-name-label">
          {{ assistantStore.activeAssistant.name }}
        </div>

        <!-- 消息正文（内含 thinking → tool_calls → content） -->
        <div class="message-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
          <!-- thinking（仅 assistant） -->
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
              <span v-if="msg.content || !chatStore.isStreaming">
                <Brain :size="12" :stroke-width="2" class="thinking-icon"/> 思考过程
              </span>
              <span v-else class="thinking-pending">
                <span class="thinking-text"><Brain :size="12" :stroke-width="2" class="thinking-icon thinking-pulse"/> 思考中<span
                    class="dots-blink"><span>.</span><span>.</span><span>.</span></span></span>
              </span>
            </button>
            <div v-if="thinkingExpanded[msg.id] && msg.thinking" class="thinking-content markdown-body"
                 v-html="renderMd(msg.thinking)"/>
          </div>

          <!-- tool_calls（历史消息中的 + 当前流式中的） -->
          <div v-if="msg.role === 'assistant'" class="tool-calls-section">
            <!-- 历史消息中的工具调用 -->
            <div
                v-for="(tc, ti) in msg.toolCalls"
                :key="'h-' + ti"
                class="tool-execution-card"
                :class="{ 'tool-error': tc.error }"
            >
              <div class="tool-execution-header" @click="toggleToolResult('h-' + ti)">
                <ChevronDown :size="12" :stroke-width="2" class="tool-chevron"
                             :class="{ rotated: toolResultExpanded['h-' + ti] }"/>
                <span class="tool-execution-icon">{{ tc.error ? '✕' : '✓' }}</span>
                <span class="tool-execution-name">{{ displayToolName(tc.toolName) }}</span>
              </div>
              <div v-if="tc.arguments || tc.result" class="tool-execution-result markdown-body"
                   :class="{ collapsed: !toolResultExpanded['h-' + ti] }"
                   v-html="renderMd(buildToolContent(tc.arguments, tc.result))">
              </div>
            </div>
            <!-- 当前流式中的工具调用（仅最新消息） -->
            <div
                v-for="(tool, ti) in msg === chatStore.messages[chatStore.messages.length - 1] ? chatStore.currentToolExecutions : []"
                :key="'s-' + ti"
                class="tool-execution-card"
                :class="{ 'tool-error': tool.error }"
            >
              <div class="tool-execution-header" @click="toggleToolResult('s-' + ti)">
                <ChevronDown :size="12" :stroke-width="2" class="tool-chevron"
                             :class="{ rotated: toolResultExpanded['s-' + ti] }"/>
                <span class="tool-execution-icon">{{ tool.done ? (tool.error ? '✕' : '✓') : '⟳' }}</span>
                <span class="tool-execution-name">{{ displayToolName(tool.toolName) }}</span>
                <span v-if="!tool.done" class="tool-execution-status">运行中...</span>
              </div>
              <div v-if="tool.done && (tool.arguments || tool.result)" class="tool-execution-result markdown-body"
                   :class="{ collapsed: !toolResultExpanded['s-' + ti] }"
                   v-html="renderMd(buildToolContent(tool.arguments, tool.result))">
              </div>
            </div>
          </div>

          <!-- content -->
          <div
              v-if="msg.content"
              class="message-content markdown-body"
              v-html="renderMd(msg.content)"
          ></div>
        </div>

        <!-- 底部区域：元数据 + 操作按钮合并 -->
        <div v-if="msg.content" class="msg-footer">
          <!-- 元数据（仅 assistant）— 桌面端完整版 -->
          <div v-if="msg.role === 'assistant' && msg.metadata"
               class="metadata-desktop">
            <template v-if="msg.metadata.timestamp && displaySettings.showMetaTime">
              <span class="meta-time">{{ formatShortTime(msg.metadata.timestamp) }}</span>
            </template>
            <template v-if="msg.metadata.durationMs != null && displaySettings.showMetaDuration">
              <span v-if="msg.metadata.timestamp && displaySettings.showMetaTime" class="meta-divider">|</span>
              <span class="meta-duration">
                {{ (msg.metadata.durationMs / 1000).toFixed(1) }}s
              </span>
            </template>
            <template
                v-if="displaySettings.showMetaTokens && (msg.metadata.totalTokens != null || msg.metadata.promptTokens != null)">
              <span
                  v-if="(msg.metadata.timestamp && displaySettings.showMetaTime) || (msg.metadata.durationMs != null && displaySettings.showMetaDuration)"
                  class="meta-divider">|</span>
              <span class="meta-tokens">
                <template v-if="msg.metadata.totalTokens != null">{{ msg.metadata.totalTokens }} tokens</template>
                <span v-if="msg.metadata.promptTokens != null" class="meta-tokens-prefix"> ←{{
                    msg.metadata.promptTokens
                  }}</span>
                <span
                    v-if="msg.metadata.completionTokens != null || (msg.metadata.cacheReadInputTokens ?? 0) > 0 || (msg.metadata.cacheWriteInputTokens ?? 0) > 0"
                    class="meta-tokens-detail">
                  <template v-if="msg.metadata.completionTokens != null"> →{{
                      msg.metadata.completionTokens
                    }}</template>
                  <template
                      v-if="(msg.metadata.cacheReadInputTokens ?? 0) > 0 || (msg.metadata.cacheWriteInputTokens ?? 0) > 0">
                    <template v-if="(msg.metadata.cacheReadInputTokens ?? 0) > 0">
                      <Eye class="cache-icon" :stroke-width="1.5"/> {{ msg.metadata.cacheReadInputTokens }}
                    </template>
                    <template v-if="(msg.metadata.cacheWriteInputTokens ?? 0) > 0">
                      <Pen class="cache-icon" :stroke-width="1.5"/> {{ msg.metadata.cacheWriteInputTokens }}
                    </template>
                  </template>
                </span>
              </span>
            </template>
            <!-- 预估费用 -->
            <template v-if="msg.metadata.costEstimate != null && displaySettings.showMetaCost">
              <span
                  v-if="(msg.metadata.timestamp && displaySettings.showMetaTime) || (msg.metadata.durationMs != null && displaySettings.showMetaDuration) || (msg.metadata.totalTokens != null && displaySettings.showMetaTokens) || (msg.metadata.promptTokens != null)"
                  class="meta-divider">|</span>
              <span class="meta-cost">{{ currencySymbol(msg.metadata.currency) }}{{
                  msg.metadata.costEstimate.toFixed(6)
                }}</span>
            </template>
          </div>

          <!-- 元数据（仅 assistant）— 移动端精简版 -->
          <div v-if="msg.role === 'assistant' && msg.metadata"
               class="metadata-compact">
            <span v-if="msg.metadata.totalTokens != null && displaySettings.showMetaTokens"
                  class="meta-tokens">{{ msg.metadata.totalTokens }}tokens</span>
            <span
                v-if="msg.metadata.totalTokens != null && displaySettings.showMetaTokens && msg.metadata.costEstimate != null && displaySettings.showMetaCost"
                class="meta-cdivider">｜</span>
            <span v-if="msg.metadata.costEstimate != null && displaySettings.showMetaCost"
                  class="meta-cost">{{ currencySymbol(msg.metadata.currency) }}{{
                msg.metadata.costEstimate.toFixed(6)
              }}</span>
            <button
                class="meta-expand-btn"
                :class="{ rotated: metaExpanded[msg.id] }"
                @click.stop="toggleMetaExpanded(msg.id)"
                :title="metaExpanded[msg.id] ? '收起详情' : '展开详情'"
            >
              <ChevronDown :size="10" :stroke-width="2"/>
            </button>
          </div>

          <!-- 操作栏（复制 + 重新生成） -->
          <div class="message-actions">
            <button
                class="action-btn"
                :data-copied="copiedMessageId === msg.id || undefined"
                @click="copyContent(msg.id, msg.content)"
                :title="copiedMessageId === msg.id ? '已复制' : '复制消息'"
            >
              <Copy v-if="copiedMessageId !== msg.id" :size="13" :stroke-width="2"/>
              <span v-else class="copied-text">已复制</span>
            </button>
            <button
                v-if="msg.role === 'assistant'"
                class="action-btn regenerate-btn"
                :disabled="chatStore.isStreaming"
                :title="chatStore.isStreaming ? '请在消息生成结束后操作' : '重新生成'"
                @click="chatStore.regenerate(chatStore.messages.indexOf(msg))"
            >
              <RefreshCw :size="13" :stroke-width="2"/>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style src="./message-list.css" scoped/>
