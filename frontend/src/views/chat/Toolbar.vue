<!--
  Toolbar.vue — 聊天视图右上角悬浮工具栏

  功能：
  - 切换宽屏/窄屏模式
  - 切换聊天模式/问答模式

  与父组件通信：
  - modelValue.wide (boolean) — 宽屏模式
  - modelValue.chat (boolean) — 聊天模式
-->
<script setup lang="ts">
import {FileText, Maximize2, MessageSquare, Minimize2} from '@lucide/vue'

defineProps<{
  wide: boolean
  chat: boolean
}>()

const emit = defineEmits<{
  'update:wide': [value: boolean]
  'update:chat': [value: boolean]
}>()
</script>

<template>
  <div class="view-toolbar">
    <button
        class="toolbar-btn"
        :title="wide ? '切换窄屏' : '切换全宽'"
        @click="emit('update:wide', !wide)"
    >
      <Maximize2 v-if="wide" :size="14" :stroke-width="1.8"/>
      <Minimize2 v-else :size="14" :stroke-width="1.8"/>
    </button>
    <button
        class="toolbar-btn"
        :title="chat ? '切换问答模式' : '切换聊天模式'"
        @click="emit('update:chat', !chat)"
    >
      <MessageSquare v-if="chat" :size="14" :stroke-width="1.8"/>
      <FileText v-else :size="14" :stroke-width="1.8"/>
    </button>
  </div>
</template>

<style scoped>
.view-toolbar {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 50;
  display: flex;
  gap: 2px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  opacity: 0.65;
  transition: opacity 0.2s, background 0.2s, box-shadow 0.2s;
}

.chat-view:hover .view-toolbar,
.view-toolbar:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.toolbar-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: background 0.15s, color 0.15s, transform 0.12s;
}

.toolbar-btn:hover {
  background: #e8eaee;
  color: #374151;
}

.toolbar-btn:active {
  transform: scale(0.92);
}
</style>
