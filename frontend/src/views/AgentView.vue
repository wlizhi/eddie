gai wen<!--
 * @author Eddie
 * @date 2026-07-04
-->

<!--
  AgentView.vue — 智能体视图（编排器）

  职责：
  - 作为智能体页面的根容器
  - 初始化加载智能体列表
  - 始终显示输入区域（AgentInputArea）
  - 有消息时展示 MessageList，无消息时展示空状态
  - 切换智能体时自动重置到新对话
-->
<script setup lang="ts">
import {nextTick, onMounted, ref, watch} from 'vue'
import {Bot} from '@lucide/vue'
import {useAgentStore} from '@/stores/agent'
import {useAgentChatStore} from '@/stores/agent-chat'
import {fetchAgentDetail} from '@/api/agent'
import {displaySettings, loadDisplaySettings} from '@/composables/useDisplaySettings'
import {useIconSize} from '@/composables/useIconSize'
import {useMobile} from '@/composables/useMobile'
import Toolbar from '@/views/chat/Toolbar.vue'
import AgentMessageList from '@/components/agent/AgentMessageList.vue'
import AgentInputArea from '@/components/agent/AgentInputArea.vue'

const agentStore = useAgentStore()
const agentChatStore = useAgentChatStore()

/** 输入框文本 */
const inputText = ref('')

const {isMobile} = useMobile()
const {iconSizeXxl} = useIconSize()

/** InputArea 组件引用 */
const inputAreaRef = ref<InstanceType<typeof AgentInputArea> | null>(null)

/** 空状态示例问题列表 */
const SUGGESTIONS = [
  '你好，请介绍一下你自己',
  '你能帮我做什么？',
  '帮我搜索一下今天的科技新闻',
  '总结一下 AI 领域的最新进展',
  '帮我写一封正式的商务邮件',
  '有什么好听的音乐推荐？',
  '今天天气怎么样？',
  '帮我制定一个学习计划',
  '解释一下什么是量子计算',
  '推荐几本值得阅读的书籍',
  '写一首关于秋天的诗',
  '帮我分析一下这个问题的思路',
]

/** 当前随机展示的示例问题（每次打开随机选 3 条）*/
const randomSuggestions = ref<string[]>([])

function shuffleSuggestions() {
  const copy = [...SUGGESTIONS]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]]
  }
  randomSuggestions.value = copy.slice(0, 4)
}

/**
 * 从当前智能体详情中同步 preferences（联网、MCP 模式）配置
 * 有配置则使用智能体的默认值，无配置则回退默认
 */
async function syncPreferredSettingsFromAgent() {
  const id = agentStore.activeId
  if (!id) {
    agentChatStore.webSearchEnabled = false
    agentChatStore.mcpToolMode = 'auto'
    return
  }
  try {
    const detail = await fetchAgentDetail(id)
    const prefs = detail.preferences ?? {}
    agentChatStore.webSearchEnabled = prefs.webSearchEnabled ?? false
    agentChatStore.mcpToolMode = (prefs.mcpToolMode ?? 'auto') as 'disabled' | 'auto' | 'manual'
    // 加载智能体已绑定的 MCP 工具列表（供手动模式选择弹窗使用）
    await agentChatStore.loadBoundMcpTools(id)
  } catch (err) {
    console.error('获取智能体详情失败:', err)
    agentChatStore.webSearchEnabled = false
    agentChatStore.mcpToolMode = 'auto'
  }
}

onMounted(async () => {
  shuffleSuggestions()
  await Promise.all([
    loadDisplaySettings(),
    agentStore.loadList(),
    agentChatStore.loadModels(),
  ])
})

// 切换智能体时同步偏好并重置对话
watch(() => agentStore.activeId, async () => {
  await syncPreferredSettingsFromAgent()
  agentChatStore.newConversation()
  inputText.value = ''
})

/** 发送消息 */
function handleSend() {
  const text = inputText.value
  if (!text.trim() || agentChatStore.isStreaming) return
  if (!agentStore.activeId) return
  agentChatStore.sendMessage(text, agentStore.activeId)
  if (!isMobile.value) {
    nextTick(() => {
      inputAreaRef.value?.focusInput()
    })
  }
}

/** 后端确认接收消息后清空输入框 */
watch(() => agentChatStore.confirmedText, (text) => {
  if (text) {
    inputText.value = ''
    agentChatStore.confirmedText = ''
    if (!isMobile.value) {
      nextTick(() => {
        inputAreaRef.value?.focusInput()
      })
    }
  }
})

/** 空状态建议问题点击 */
function onSelectSuggestion(text: string) {
  inputText.value = text
}
</script>

<template>
  <div class="agent-view" :class="{ narrow: !displaySettings.wideMode, 'qa-mode': !displaySettings.chatMode }">
    <Toolbar/>
    <!-- 消息列表 / 空状态 -->
    <AgentMessageList v-if="agentChatStore.hasMessages" :qa-mode="!displaySettings.chatMode"/>
    <div v-else class="agent-empty">
      <Bot
          :size="iconSizeXxl"
          :stroke-width="1.5"
          class="empty-icon"
      />
      <h2>{{ agentStore.activeAgent?.name || '智能体' }}</h2>
      <p class="empty-hint">
        {{
          agentStore.activeAgent
              ? (agentStore.activeAgent.description || '开始与智能体对话')
              : '选择左侧列表中的智能体，或创建新的智能体开始使用'
        }}
      </p>
      <div v-if="agentStore.activeAgent" class="suggestions">
        <div
            v-for="(item, idx) in randomSuggestions"
            :key="idx"
            class="suggestion-card"
            @click="onSelectSuggestion(item)"
        >
          {{ item }}
        </div>
      </div>
    </div>

    <!-- 输入区域（始终显示） -->
    <AgentInputArea
        ref="inputAreaRef"
        v-model="inputText"
        @send="handleSend"
    />
  </div>
</template>

<style scoped>
.agent-view {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* 窄屏模式 */
.agent-view.narrow :deep(.agent-message-list),
.agent-view.narrow :deep(.agent-input-area) {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

/* 全宽模式：消息行按父容器百分比自适应 */
.agent-view:not(.narrow) :deep(.agent-message-list .message-row) {
  max-width: min(82%, 1000px);
}

/* 工具栏悬停显示 */
.agent-view:hover :deep(.view-toolbar) {
  opacity: 1;
  background: var(--bg-secondary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

/* ===== 空状态 ===== */
.agent-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-6);
  padding: var(--space-20);
}

.empty-icon {
  color: var(--text-tertiary);
  margin-bottom: var(--space-4);
}

h2 {
  font-size: var(--font-size-title);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.empty-hint {
  color: var(--text-tertiary);
  font-size: var(--font-size-base);
  margin: 0;
  text-align: center;
  max-width: 20rem;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-12);
  max-width: 28rem;
  width: 100%;
}

.suggestion-card {
  padding: var(--space-6) var(--space-8);
  border: 1px solid var(--border-light);
  border-radius: var(--space-4);
  cursor: pointer;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  text-align: center;
  transition: background 0.15s, border-color 0.15s;
  line-height: 1.5;
}

.suggestion-card:hover {
  background: var(--bg-hover);
  border-color: var(--border-hover);
}
</style>
