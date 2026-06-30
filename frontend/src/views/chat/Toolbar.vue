<!--
 * @author Eddie
 * @date 2026-06-21
-->

<!--
  Toolbar.vue — 聊天视图右上角悬浮工具栏

  功能：
  - 切换宽屏/窄屏模式
  - 切换聊天模式/问答模式

  数据源：全局 displaySettings（持久化）
-->
<script setup lang="ts">
import {FileText, Maximize2, MessageSquare, Minimize2} from '@lucide/vue'
import {displaySettings, saveDisplaySettings} from '@/composables/useDisplaySettings'
import {useIconSize} from '@/composables/useIconSize'

const {iconSizeSm} = useIconSize()

function toggleWide() {
  displaySettings.wideMode = !displaySettings.wideMode
  saveDisplaySettings()
}

function toggleChat() {
  displaySettings.chatMode = !displaySettings.chatMode
  saveDisplaySettings()
}
</script>

<template>
  <div class="view-toolbar">
    <button
        class="toolbar-btn"
        :title="displaySettings.wideMode ? '切换窄屏' : '切换全宽'"
        @click="toggleWide"
    >
      <Maximize2 v-if="displaySettings.wideMode" :size="iconSizeSm" :stroke-width="1.8"/>
      <Minimize2 v-else :size="iconSizeSm" :stroke-width="1.8"/>
    </button>
    <button
        class="toolbar-btn"
        :title="displaySettings.chatMode ? '切换问答模式' : '切换聊天模式'"
        @click="toggleChat"
    >
      <MessageSquare v-if="displaySettings.chatMode" :size="iconSizeSm" :stroke-width="1.8"/>
      <FileText v-else :size="iconSizeSm" :stroke-width="1.8"/>
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
  background: var(--bg-mask);
  border: 1px solid var(--border-lighter);
  border-radius: 8px;
  opacity: 0.65;
  transition: opacity 0.2s, background 0.2s, box-shadow 0.2s;
}

.chat-view:hover .view-toolbar,
.view-toolbar:hover {
  opacity: 1;
  background: var(--bg-secondary);
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
  color: var(--text-quaternary);
  transition: background 0.15s, color 0.15s, transform 0.12s;
}

.toolbar-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.toolbar-btn:active {
  transform: scale(0.92);
}
</style>
