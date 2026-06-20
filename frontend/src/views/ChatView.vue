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
import {nextTick, onMounted, ref} from 'vue'
import {useChatStore} from '@/stores/chat'
import Toolbar from '@/views/chat/Toolbar.vue'
import MessageList from '@/views/chat/MessageList.vue'
import EmptyState from '@/views/chat/EmptyState.vue'
import InputArea from '@/views/chat/InputArea.vue'

const chatStore = useChatStore()

/** 宽屏模式 */
const isWideMode = ref(false)
/** 聊天模式（false=问答模式左对齐） */
const isChatMode = ref(true)
/** 输入框文本 */
const inputText = ref('')

/** InputArea 组件引用，用于发送后保持焦点 */
const inputAreaRef = ref<InstanceType<typeof InputArea> | null>(null)

onMounted(() => {
  chatStore.loadModels()
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
  <div class="chat-view" :class="{ narrow: !isWideMode, 'qa-mode': !isChatMode }">
    <Toolbar v-model:wide="isWideMode" v-model:chat="isChatMode"/>

    <MessageList v-if="chatStore.hasMessages" :qa-mode="!isChatMode"/>

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
</style>
