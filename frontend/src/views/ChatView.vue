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
import Toolbar from '@/views/chat/Toolbar.vue'
import MessageList from '@/views/chat/MessageList.vue'
import EmptyState from '@/views/chat/EmptyState.vue'
import InputArea from '@/views/chat/InputArea.vue'

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 输入框文本 */
const inputText = ref('')

/** InputArea 组件引用，用于发送后保持焦点 */
const inputAreaRef = ref<InstanceType<typeof InputArea> | null>(null)

onMounted(async () => {
  // 并行加载显示设置、模型列表和助手列表
  await Promise.all([
    loadDisplaySettings(),
    chatStore.loadModels(),
    assistantStore.loadList(),
  ])
  // 助手列表加载完成后同步第一个助手的模型到聊天区
  syncModelFromAssistant()
  // 同步当前助手的思考模式配置
  await syncThinkingModeFromAssistant()
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
 * 从当前助手详情中同步 thinkingMode 配置
 * 有配置则使用助手的默认值，无配置则回退 auto
 */
async function syncThinkingModeFromAssistant() {
  const id = assistantStore.activeId
  if (!id) {
    chatStore.syncThinkingMode('auto')
    return
  }
  try {
    const detail = await fetchAssistantDetail(id)
    const tm = detail.modelParams?.thinkingMode
    chatStore.syncThinkingMode(tm ?? 'auto')
  } catch (err) {
    console.error('获取助手详情失败:', err)
    chatStore.syncThinkingMode('auto')
  }
}

// 切换助手时自动同步模型、思考模式并清空聊天状态（等同于新建会话）
watch(() => assistantStore.activeId, async () => {
  syncModelFromAssistant()
  await syncThinkingModeFromAssistant()
  chatStore.newConversation()
})

/** 发送消息 */
function handleSend() {
  const text = inputText.value
  if (!text.trim() || chatStore.isStreaming) return
  inputText.value = ''
  chatStore.sendMessage(text)
  // 发送后保持输入框焦点
  nextTick(() => {
    inputAreaRef.value?.focusInput()
  })
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
