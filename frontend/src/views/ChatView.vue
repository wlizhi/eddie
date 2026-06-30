<!--
 * @author Eddie
 * @date 2026-06-20
-->

<!--
  ChatView.vue — 聊天视图（编排器）

  职责：
  - 作为聊天页面的根容器，协调各子组件
  - 管理仅与编排相关的局部状态（宽屏/问答模式、输入框文本）
  - 数据由子组件直接从 Pinia store 读取

  子组件：
  - Toolbar — 右上角视图模式切换
  - MessageList — 消息列表（含 thinking、元数据）
  - EmptyState — 空状态引导
  - InputArea — 底部输入区域（含模型选择器）
-->
<script setup lang="ts">
import {nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {useAssistantStore} from '@/stores/assistant'
import {fetchAssistantDetail} from '@/api/assistant'
import {displaySettings, loadDisplaySettings} from '@/composables/useDisplaySettings'
import {useMobile} from '@/composables/useMobile'
import Toolbar from '@/views/chat/Toolbar.vue'
import MessageList from '@/views/chat/MessageList.vue'
import EmptyState from '@/views/chat/EmptyState.vue'
import InputArea from '@/views/chat/InputArea.vue'

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 输入框文本 */
const inputText = ref('')

const {isMobile} = useMobile()

/** InputArea 组件引用，用于发送后保持焦点 */
const inputAreaRef = ref<InstanceType<typeof InputArea> | null>(null)

onMounted(async () => {
  // 并行加载基础数据（显示设置、模型列表、助手列表）
  // loadList() 会自动设置 activeId → 触发下面的 watch 来同步思考和 MCP 工具
  await Promise.all([
    loadDisplaySettings(),
    chatStore.loadModels(),
    assistantStore.loadList(),
  ])
})

/**
 * 将当前选中助手的模型同步到聊天区
 * 页面刷新 / 切换助手时自动调用
 */
function syncModelFromAssistant() {
  const active = assistantStore.activeAssistant
  if (active) {
    chatStore.selectModel(active.modelId, active.providerId)
  }
}

/**
 * 从当前助手详情中同步 thinkingMode + preferences 配置
 * 有配置则使用助手的默认值，无配置则回退默认
 */
async function syncPreferredSettingsFromAssistant() {
  const id = assistantStore.activeId
  if (!id) {
    chatStore.syncThinkingMode('auto')
    chatStore.webSearchEnabled = false
    chatStore.mcpToolMode = 'auto'
    return
  }
  try {
    const detail = await fetchAssistantDetail(id)
    // 思考模式
    const tm = detail.modelParams?.thinkingMode
    chatStore.syncThinkingMode(tm ?? 'auto')
    // 助手偏好（联网、MCP 默认模式）
    const prefs = detail.preferences ?? {}
    chatStore.webSearchEnabled = prefs.webSearchEnabled ?? false
    chatStore.mcpToolMode = (prefs.mcpToolMode ?? 'auto') as 'disabled' | 'auto' | 'manual'
  } catch (err) {
    console.error('获取助手详情失败:', err)
    chatStore.syncThinkingMode('auto')
    chatStore.webSearchEnabled = false
    chatStore.mcpToolMode = 'auto'
  }
}

// 切换助手时自动同步模型、偏好设置、工具列表并清空聊天状态
watch(() => assistantStore.activeId, async (newId) => {
  syncModelFromAssistant()
  await Promise.all([
    syncPreferredSettingsFromAssistant(),
    newId ? chatStore.loadBoundMcpTools(newId) : Promise.resolve(),
  ])
  chatStore.newConversation()
})

/** 发送消息 */
function handleSend() {
  const text = inputText.value
  if (!text.trim() || chatStore.isStreaming) return
  inputText.value = ''
  chatStore.sendMessage(text)
  // 桌面端发送后保持输入框焦点，移动端不聚焦以避免唤起键盘
  if (!isMobile.value) {
    nextTick(() => {
      inputAreaRef.value?.focusInput()
    })
  }
}

/** 空状态建议问题点击 */
function onSelectSuggestion(text: string) {
  inputText.value = text
}
</script>

<template>
  <div class="chat-view" :class="{ narrow: !displaySettings.wideMode, 'qa-mode': !displaySettings.chatMode }">
    <Toolbar/>

    <MessageList v-if="chatStore.hasMessages" :qa-mode="!displaySettings.chatMode"/>

    <EmptyState v-else @select-suggestion="onSelectSuggestion"/>

    <InputArea
        ref="inputAreaRef"
        v-model="inputText"
        @send="handleSend"
    />
  </div>
</template>

<style scoped>
/* ===== 容器布局 ===== */
.chat-view {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ===== 窄屏模式：子组件根元素居中缩窄 ===== */
.chat-view.narrow :deep(.message-list),
.chat-view.narrow :deep(.input-area) {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

/* ===== 全宽模式：消息行按父容器百分比自适应 ===== */
.chat-view:not(.narrow) :deep(.message-row) {
  max-width: min(82%, 1000px);
}
</style>
