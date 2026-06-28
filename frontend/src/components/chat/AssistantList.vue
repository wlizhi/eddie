<script setup lang="ts">
import {computed, ref} from 'vue'
import {ChevronDown, GripVertical, Plus, Settings} from '@lucide/vue'
import {useAssistantStore} from '@/stores/assistant'
import {batchSortAssistant} from '@/api/assistant'
import {useDragSort} from '@/composables/useDragSort'
import {getEffectiveFontSize} from '@/composables/useDisplaySettings'
import {useIconSize} from '@/composables/useIconSize'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AssistantDialog from '../assistant/AssistantDialog.vue'

const {iconSizeXs, iconSizeSm} = useIconSize()

const assistantStore = useAssistantStore()

const DEFAULT_SHOWN = 3
const assistantListCollapsed = ref(false)
const showAllAssistants = ref(true)
const editAssistantId = ref<number | null>(null)
const showCreateAssistant = ref(false)

// 拖拽排序
const {dragIndex, dragOverIndex, onDragStart, onDragOver, onDragLeave, onDrop} =
    useDragSort(() => assistantStore.list, batchSortAssistant, () => {
      assistantStore.loadList(true, true)
    })

/** 头像随字体大小自适应（1.8 倍率，适合紧凑侧边栏） */
const avatarSize = computed(() => Math.round(getEffectiveFontSize() * 1.8))

const displayedAssistants = computed(() => {
  const items = assistantStore.list
  return showAllAssistants.value ? items : items.slice(0, DEFAULT_SHOWN)
})
</script>

<template>
  <!-- 助手列表折叠/展开按钮 -->
  <button class="collapse-assistant-btn" @click="assistantListCollapsed = !assistantListCollapsed">
    <span class="create-trigger" title="创建助手" @click.stop="showCreateAssistant = true">
      <Plus :size="iconSizeSm" :stroke-width="2.5"/>
    </span>
    <ChevronDown :size="iconSizeSm" :stroke-width="2" class="collapse-icon"
                 :class="{ rotated: !assistantListCollapsed }"/>
    <span>{{ assistantListCollapsed ? '展开助手列表' : '收起助手列表' }}</span>
  </button>

  <!-- 助手列表区域 -->
  <div class="assistant-section" v-show="!assistantListCollapsed">
    <div class="assistant-list">
      <div
          v-for="(assistant, index) in displayedAssistants"
          :key="assistant.id"
          class="assistant-item"
          :class="{
            active: assistantStore.activeId === assistant.id,
            disabled: assistant.enabled !== 1,
            'drag-over': dragOverIndex === index,
          }"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover="onDragOver($event, index)"
          @dragleave="onDragLeave"
          @drop.prevent="onDrop"
          @dragend="dragIndex = null; dragOverIndex = null"
          @click="assistantStore.select(assistant.id)"
      >
        <span class="drag-handle"><GripVertical :size="iconSizeXs" :stroke-width="1.5"/></span>
        <AssistantAvatar :name="assistant.name" :avatar="assistant.avatar" :size="avatarSize"/>
        <span class="assistant-name">{{ assistant.name }}</span>
        <button
            class="assistant-settings"
            title="助手设置"
            @click.stop="editAssistantId = assistant.id"
        >
          <Settings :size="iconSizeSm" :stroke-width="2"/>
        </button>
      </div>

      <!-- 展开/收起 -->
      <button
          v-if="assistantStore.list.length > DEFAULT_SHOWN"
          class="toggle-btn"
          @click="showAllAssistants = !showAllAssistants"
      >
        {{ showAllAssistants ? '▲ 收起' : `>>> 展示更多 (${assistantStore.list.length - DEFAULT_SHOWN})` }}
      </button>
    </div>
  </div>

  <!-- 助手设置/创建弹窗 -->
  <AssistantDialog v-model:assistant-id="editAssistantId" v-model:create-visible="showCreateAssistant"/>
</template>

<style scoped>
/* ===== 折叠助手列表按钮 ===== */
.collapse-assistant-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  padding: 7px 5px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-tertiary);
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-default);
}

@media (hover: hover) {
  .collapse-assistant-btn:hover {
    background: var(--bg-hover);
    color: var(--text-quaternary);
  }
}

/* ===== 新建按钮（绝对定位在折叠按钮最左侧） ===== */
.create-trigger {
  position: absolute;
  left: 5px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 5px;
  background: transparent;
  cursor: pointer;
  color: var(--text-tertiary);
  transition: background 0.1s, color 0.1s;
}

@media (hover: hover) {
  .create-trigger:hover {
    background: var(--accent-light-bg);
    color: var(--accent-default);
  }
}

.collapse-icon {
  transition: transform 0.2s;
}

.collapse-icon.rotated {
  transform: rotate(0deg);
}

/* ===== 助手列表 ===== */
.assistant-section {
  flex-shrink: 0;
}

.assistant-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 8px;
}

.assistant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: background 0.15s;
  touch-action: manipulation;
}

@media (hover: hover) {
  .assistant-item:hover {
    background: var(--bg-hover);
  }
}

.assistant-item.active {
  background: var(--accent-light-bg);
}

/* 禁用的助手：降低透明度，灰色调 */
.assistant-item.disabled {
  opacity: 0.55;
}

@media (hover: hover) {
  .assistant-item.disabled:hover {
    background: #f9fafb;
  }
}

/* 拖拽排序 */
.drag-handle {
  display: flex;
  align-items: center;
  color: var(--text-quaternary);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
  margin-right: -2px;
}

@media (hover: hover) {
  .assistant-item:hover .drag-handle {
    color: var(--text-secondary);
  }
}

.assistant-item.drag-over {
  border-top: 2px solid var(--accent-default);
  border-radius: 0;
}

.assistant-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-settings {
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-quaternary);
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
  flex-shrink: 0;
}

@media (hover: hover) {
  .assistant-item:hover .assistant-settings {
    opacity: 1;
  }
}

/* 触屏设备：设置按钮始终可见 */
@media (hover: none) {
  .assistant-settings {
    opacity: 1;
  }
}

@media (hover: hover) {
  .assistant-settings:hover {
    color: var(--text-secondary);
    background: var(--bg-hover);
  }
}

/* 展开/收起按钮 */
.toggle-btn {
  display: block;
  width: 100%;
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: var(--font-size-small);
  color: var(--text-quaternary);
  text-align: left;
  transition: background 0.15s, color 0.15s;
}

@media (hover: hover) {
  .toggle-btn:hover {
    background: var(--bg-hover);
    color: var(--accent-default);
  }
}
</style>
