<!--
 * @author Eddie
 * @date 2026-06-21
-->

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
import {ChevronDown, Copy, Eye, Loader, Pen, RefreshCw} from '@lucide/vue'
import {formatShortTime} from '@/utils/format'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'
import {displaySettings, getEffectiveFontSize} from '@/composables/useDisplaySettings'
import {approveTool} from '@/api/chat'
import {showToast} from '@/composables/useToast'
import AgentThinkingBlock from '@/components/chat/AgentThinkingBlock.vue'
import AgentToolCard from '@/components/chat/AgentToolCard.vue'
import AgentContentBlock from '@/components/chat/AgentContentBlock.vue'

defineProps<{
  qaMode: boolean
}>()

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 消息容器 DOM，用于自动滚动 */
const messageListRef = ref<HTMLElement | null>(null)

/** 记录加载更多前的高度，用于保持滚动位置 */
let prevScrollHeight = 0

/** 随字体大小动态变化的头像大小（基准字号 × 2.5，与 --avatar-size 保持一致） */
const avatarSize = computed(() => Math.round(getEffectiveFontSize() * 2.2))

/** 用户是否已手动上滑（打断自动滚动） */
const userScrolledAway = ref(false)

/** 审批工具调用 */
async function handleApprove(tool: { msgId?: number; seq?: number }, approved: boolean): Promise<void> {
    const msgId = tool.msgId
    if (msgId == null) {
        showToast('缺少消息 ID，无法审批', 'error')
        return
    }
    try {
        await approveTool(msgId, approved, tool.seq)
        // 审批接口返回后立即更新本地状态，不等工具执行完毕的 SSE 事件
        const target = chatStore.currentToolExecutions.find(
            t => t.msgId === msgId && t.seq === tool.seq && !t.done
        )
        if (target) {
            if (approved) {
                target.pendingApproval = false  // 按钮消失，显示"运行中..."
            } else {
                target.done = true
                target.rejected = true
                target.pendingApproval = false
            }
        }
        showToast(approved ? '已批准' : '已拒绝', 'success')
    } catch (err) {
        showToast(`审批失败: ${(err as Error).message}`, 'error')
    }
}

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
          <span v-if="msg.modelName && displaySettings.showMetaModel" class="assistant-model-name">
            ({{ msg.modelName }})
          </span>
        </div>

        <!-- 消息正文（内含 thinking → tool_calls → content） -->
        <div class="message-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
          <!-- thinking（仅 assistant） -->
          <AgentThinkingBlock
              v-if="msg.role === 'assistant'"
              :thinking="msg.thinking || ''"
              :is-streaming="chatStore.isStreaming"
              :is-last="msg === chatStore.messages[chatStore.messages.length - 1]"
              :has-content="!!msg.content"
          />

          <!-- tool_calls（历史消息中的 + 当前流式中的） -->
          <div v-if="msg.role === 'assistant' &&
                ((msg.toolCalls && msg.toolCalls.length > 0) ||
                 (msg === chatStore.messages[chatStore.messages.length - 1] && chatStore.currentToolExecutions.length > 0))"
               class="tool-calls-section">
            <!-- 历史消息中的工具调用 -->
            <AgentToolCard
                v-for="(tc, ti) in msg.toolCalls"
                :key="'h-' + msg.id + '-' + ti"
                :tool-call="tc"
            />
            <!-- 当前流式中的工具调用（仅最新消息） -->
            <AgentToolCard
                v-for="(tool, ti) in msg === chatStore.messages[chatStore.messages.length - 1] ? chatStore.currentToolExecutions : []"
                :key="'s-' + msg.id + '-' + ti"
                :tool-call="tool"
                @approve="(approved) => handleApprove(tool, approved)"
            />
          </div>

          <!-- content -->
          <AgentContentBlock
              v-if="msg.content"
              :content="msg.content"
          />
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

<!-- 非 scoped 样式：覆盖 v-html 渲染的 <a> 标签（scoped 属性无法穿透） -->
<style>
.user-bubble .markdown-body a {
    color: var(--msg-user-text);
    text-decoration: underline;
    text-underline-offset: 2px;
    opacity: 0.92;
    transition: opacity 0.15s;
}
.user-bubble .markdown-body a:hover {
    opacity: 1;
}
</style>
