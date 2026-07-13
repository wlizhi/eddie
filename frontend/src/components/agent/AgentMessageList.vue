<!--
 * @author Eddie
 * @date 2026-07-04
-->

<!--
  AgentMessageList.vue — 智能体消息列表

  功能：
  - 渲染所有聊天消息（用户 + 助手）
  - 助手的思考过程（thinking）折叠/展开
  - 智能体特有的轮次指示器（round markers）
  - 里程碑事件展示
  - 助手消息的元数据显示（时间、耗时、token 用量）
  - 工具调用卡片折叠/展开
  - 流式响应时自动滚动到底部
  - 滚动到顶部时自动加载更早的消息（游标分页）

  数据来源：直接从 Pinia store (useAgentChatStore) 读取
-->
<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useAgentChatStore} from '@/stores/agent-chat'
import {useAgentStore} from '@/stores/agent'
import {ChevronDown, Copy, Eye, Loader, Pen, RefreshCw} from '@lucide/vue'
import {formatShortTime} from '@/utils/format'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'
import {displaySettings, getEffectiveFontSize} from '@/composables/useDisplaySettings'
import {approveTool} from '@/api/agent-chat'
import {showToast} from '@/composables/useToast'
import AgentPlanTodoList from '@/components/agent/AgentPlanTodoList.vue'
import AgentThinkingBlock from '@/components/chat/AgentThinkingBlock.vue'
import AgentToolCard from '@/components/chat/AgentToolCard.vue'
import AgentContentBlock from '@/components/chat/AgentContentBlock.vue'

const agentChatStore = useAgentChatStore()
const agentStore = useAgentStore()

/** QA 模式：用户消息左对齐 */
defineProps({
  qaMode: {
    type: Boolean,
    default: false,
  },
})

/** 消息容器 DOM */
const messageListRef = ref<HTMLElement | null>(null)

/** 记录加载更多前的高度，用于保持滚动位置 */
let prevScrollHeight = 0

/** 随字体大小动态变化的头像大小 */
const avatarSize = computed(() => Math.round(getEffectiveFontSize() * 2.2))

/** 用户是否已手动上滑（打断自动滚动） */
const userScrolledAway = ref(false)

function isNearBottom(): boolean {
  const el = messageListRef.value
  if (!el) return true
  return el.scrollTop + el.clientHeight >= el.scrollHeight - 100
}

async function scrollToBottom() {
  userScrolledAway.value = false
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function scrollToBottomIfNeeded() {
  if (userScrolledAway.value) return
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

/** 审批工具调用 */
async function handleApprove(tool: { toolName: string; msgId?: number; stepId?: number | null; seq?: number }, approved: boolean): Promise<void> {
    const msgId = tool.msgId
    if (msgId == null) {
        showToast('缺少消息 ID，无法审批', 'error')
        return
    }
    try {
        await approveTool(msgId, tool.toolName, approved, tool.stepId, tool.seq)
        // 审批接口返回后立即更新本地状态，不等工具执行完毕的 SSE 事件
        const target = agentChatStore.currentToolExecutions.find(
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

// 消息列表变化
watch(
    () => agentChatStore.messages.length,
    (_newLen, _oldLen) => {
      if (agentChatStore.isStreaming) {
        scrollToBottom()
        return
      }
      scrollToBottomIfNeeded()
    },
)
// 流式内容变化
watch(
    () => agentChatStore.currentAnswer,
    () => scrollToBottomIfNeeded(),
)
// 切换会话
watch(
    () => agentChatStore.currentConversationId,
    () => scrollToBottom(),
)
// metadata 到达
watch(
    () => agentChatStore.currentMetadata,
    () => scrollToBottomIfNeeded(),
)

onMounted(() => {
  scrollToBottom()
})

/** 跟踪每条消息的 metadata 展开状态（移动端折叠详情） */
const metaExpanded = ref<Record<string, boolean>>({})

/** 切换 metadata 展开/折叠 */
function toggleMetaExpanded(msgId: string) {
  metaExpanded.value[msgId] = !metaExpanded.value[msgId]
}

/** 复制消息内容 */
const copiedMessageId = ref<string | null>(null)

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

/** 币种符号 */
const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$', CNY: '¥', EUR: '€', GBP: '£', JPY: '¥', KRW: '₩', HKD: 'HK$', TWD: 'NT$', SGD: 'S$',
}

function currencySymbol(currency?: string | null): string {
  if (!currency) return '$'
  return CURRENCY_SYMBOLS[currency] ?? currency + ' '
}

/** 滚动事件——接近顶部时触发加载更早消息 */
function onScroll() {
  const el = messageListRef.value
  if (!el) return

  if (!isNearBottom()) {
    userScrolledAway.value = true
  }

  if (el.scrollTop <= 50 && agentChatStore.hasMoreMessages && !agentChatStore.isLoadingMore) {
    prevScrollHeight = el.scrollHeight
    agentChatStore.loadMoreMessages().then(() => {
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
  <div ref="messageListRef" class="agent-message-list" :class="{'qa-mode': qaMode}" @scroll="onScroll">
    <!-- 顶部加载指示器 -->
    <div v-if="agentChatStore.isLoadingMore" class="load-more-indicator">
      <Loader :size="16" class="spinner"/>
      <span>加载更早的消息...</span>
    </div>
    <div v-else-if="!agentChatStore.hasMoreMessages && agentChatStore.messages.length > 0" class="no-more-hint">
      已加载全部消息
    </div>

    <!-- ===== 消息遍历 ===== -->
    <template v-for="(msg, idx) in agentChatStore.messages" :key="msg.id">
      <div
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
                v-if="agentStore.activeAgent"
                :name="agentStore.activeAgent.name"
                :avatar="agentStore.activeAgent.avatar"
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
          <!-- 助手名称 + 轮次 -->
          <div v-if="msg.role === 'assistant'" class="assistant-name-label">
            <span v-if="agentStore.activeAgent">{{ agentStore.activeAgent.name }}</span>
            <span v-if="msg.modelName && displaySettings.showMetaModel" class="assistant-model-name">
              ({{ msg.modelName }})
            </span>
          </div>

          <!-- 消息气泡 -->
          <div class="message-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
            <!-- ===== 按轮次渲染（agent 流式场景） ===== -->
            <template v-if="msg.rounds && msg.rounds.length > 0">
              <template v-for="(round, ri) in msg.rounds" :key="'r-' + ri">
                <!-- thinking（每轮次独立折叠） -->
                <AgentThinkingBlock
                    v-if="msg.role === 'assistant'"
                    :thinking="round.thinking"
                    :is-streaming="agentChatStore.isStreaming"
                    :is-last="msg === agentChatStore.messages[agentChatStore.messages.length - 1] && ri === msg.rounds.length - 1"
                    :has-content="!!round.content"
                />

                <!-- tool_calls（每轮次独立） -->
                <div v-if="msg.role === 'assistant' && round.toolCalls && round.toolCalls.length > 0"
                     class="tool-calls-section">
                  <AgentToolCard
                      v-for="(tc, ti) in round.toolCalls"
                      :key="'r-' + msg.id + '-' + ri + '-' + ti"
                      :tool-call="tc"
                      @approve="(approved) => handleApprove(tc, approved)"
                  />
                </div>

                <!-- content（每轮次独立） -->
                <AgentContentBlock
                    v-if="round.content"
                    :content="round.content"
                />
              </template>
            </template>

            <!-- ===== 兼容：无 rounds 的历史消息 ===== -->
            <template v-else>
              <!-- thinking -->
              <AgentThinkingBlock
                  v-if="msg.role === 'assistant'"
                  :thinking="msg.thinking || ''"
                  :is-streaming="agentChatStore.isStreaming"
                  :is-last="msg === agentChatStore.messages[agentChatStore.messages.length - 1]"
                  :has-content="!!msg.content"
              />

              <!-- tool_calls -->
              <div v-if="msg.role === 'assistant' &&
                    ((msg.toolCalls && msg.toolCalls.length > 0) ||
                     (msg === agentChatStore.messages[agentChatStore.messages.length - 1] && agentChatStore.currentToolExecutions.length > 0))"
                   class="tool-calls-section">
                <!-- 历史消息中的工具调用 -->
                <AgentToolCard
                    v-for="(tc, ti) in msg.toolCalls"
                    :key="'h-' + msg.id + '-' + ti"
                    :tool-call="tc"
                />
                <!-- 当前流式中的工具调用（仅最新消息） -->
                <AgentToolCard
                    v-for="(tool, ti) in msg === agentChatStore.messages[agentChatStore.messages.length - 1] ? agentChatStore.currentToolExecutions : []"
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
            </template>

            <!-- 计划清单（仅 assistant，在所有轮次内容之后自然追加） -->
            <template v-if="msg.role === 'assistant'">
              <!-- 计划清单生成中 -->
              <div
                  v-if="agentChatStore.isPlanGenerating && msg === agentChatStore.messages[agentChatStore.messages.length - 1]"
                  class="plan-generating"
              >
                <Loader :size="12" class="spinner"/>
                <span class="plan-text">任务规划中<span
                    class="dots-blink"><span>.</span><span>.</span><span>.</span></span></span>
              </div>
              <!-- 计划清单内容 -->
              <AgentPlanTodoList
                  v-if="msg.taskPlan"
                  :plan="msg.taskPlan"
              />
              <!-- 任务完成结果摘要 -->
              <AgentContentBlock
                  v-if="msg.taskPlan?.status === 'completed' && msg.taskPlan?.result"
                  :content="msg.taskPlan.result"
              />
            </template>
          </div>

          <!-- 底部：元数据 + 操作按钮 -->
          <div class="msg-footer">
            <!-- 元数据（仅 agent）— 桌面端完整版 -->
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

            <!-- 元数据（仅 agent）— 移动端精简版 -->
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

            <div v-if="msg.content" class="message-actions">
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
                  :disabled="agentChatStore.isStreaming"
                  :title="agentChatStore.isStreaming ? '请在消息生成结束后操作' : '重新生成'"
                  @click="agentChatStore.regenerate(agentChatStore.messages.indexOf(msg))"
              >
                <RefreshCw :size="13" :stroke-width="2"/>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 当前轮次指示（流式响应时显示） ===== -->
    <div v-if="agentChatStore.isStreaming && agentChatStore.currentRound > 0" class="round-indicator">
      <RefreshCw :size="12" class="round-icon" />
      <span :key="agentChatStore.currentRound" class="round-number">{{ agentChatStore.currentRound }}</span>
    </div>
  </div>
</template>

<style src="./agent-message-list.css" scoped/>

<!-- 非 scoped 样式 -->
<style>
.assistant-bubble .markdown-body a {
  color: var(--msg-assistant-text);
  text-decoration: underline;
  text-underline-offset: 2px;
  opacity: 0.92;
  transition: opacity 0.15s;
}

.assistant-bubble .markdown-body a:hover {
  opacity: 1;
}

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
