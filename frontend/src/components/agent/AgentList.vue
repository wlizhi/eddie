<!--
 * @author Eddie
 * @date 2026-07-04
 -->

<script setup lang="ts">
import {computed, ref} from 'vue'
import {ChevronDown, GripVertical, Plus, Settings} from '@lucide/vue'
import {useAgentStore} from '@/stores/agent'
import {batchSortAgent} from '@/api/agent'
import {useDragSort} from '@/composables/useDragSort'
import {getEffectiveFontSize} from '@/composables/useDisplaySettings'
import {useIconSize} from '@/composables/useIconSize'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AgentDialog from './AgentDialog.vue'

const {iconSizeXs, iconSizeSm} = useIconSize()

const agentStore = useAgentStore()

const DEFAULT_SHOWN = 3
const agentListCollapsed = ref(false)
const showAllAgents = ref(false)
const editAgentId = ref<number | null>(null)
const showCreateAgent = ref(false)

// 拖拽排序
const {dragIndex, dragOverIndex, onDragStart, onDragOver, onDragLeave, onDrop} =
    useDragSort(() => agentStore.list, batchSortAgent, () => {
      agentStore.loadList(true, true)
    })

/** 头像随字体大小自适应（1.8 倍率，适合紧凑侧边栏） */
const avatarSize = computed(() => Math.round(getEffectiveFontSize() * 1.8))

const displayedAgents = computed(() => {
  const items = agentStore.list
  return showAllAgents.value ? items : items.slice(0, DEFAULT_SHOWN)
})
</script>

<template>
  <!-- 智能体列表折叠/展开按钮 -->
  <button class="collapse-agent-btn" @click="agentListCollapsed = !agentListCollapsed">
    <span class="create-trigger" title="创建智能体" @click.stop="showCreateAgent = true">
      <Plus :size="iconSizeSm" :stroke-width="2.5"/>
    </span>
    <ChevronDown :size="iconSizeSm" :stroke-width="2" class="collapse-icon"
                 :class="{ rotated: !agentListCollapsed }"/>
    <span>{{ agentListCollapsed ? '展开智能体列表' : '收起智能体列表' }}</span>
  </button>

  <!-- 智能体列表区域 -->
  <div class="agent-section" v-show="!agentListCollapsed">
    <div class="agent-list">
      <div
          v-for="(agent, index) in displayedAgents"
          :key="agent.id"
          class="agent-item"
          :class="{
            active: agentStore.activeId === agent.id,
            disabled: agent.enabled !== 1,
            'drag-over': dragOverIndex === index,
          }"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover="onDragOver($event, index)"
          @dragleave="onDragLeave"
          @drop.prevent="onDrop"
          @dragend="dragIndex = null; dragOverIndex = null"
          @click="agentStore.select(agent.id)"
      >
        <span class="drag-handle"><GripVertical :size="iconSizeXs" :stroke-width="1.5"/></span>
        <AssistantAvatar :name="agent.name" :avatar="agent.avatar" :size="avatarSize"/>
        <span class="agent-name">{{ agent.name }}</span>
        <button
            class="agent-settings"
            title="智能体设置"
            @click.stop="editAgentId = agent.id"
        >
          <Settings :size="iconSizeSm" :stroke-width="2"/>
        </button>
      </div>

      <!-- 展开/收起 -->
      <button
          v-if="agentStore.list.length > DEFAULT_SHOWN"
          class="toggle-btn"
          @click="showAllAgents = !showAllAgents"
      >
        {{ showAllAgents ? '▲ 收起' : `>>> 展示更多 (${agentStore.list.length - DEFAULT_SHOWN})` }}
      </button>
    </div>
  </div>

  <!-- 智能体设置/创建弹窗 -->
  <AgentDialog v-model:agent-id="editAgentId" v-model:create-visible="showCreateAgent"/>
</template>

<style scoped>
/* ===== 折叠智能体列表按钮 ===== */
.collapse-agent-btn {
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
  .collapse-agent-btn:hover {
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

/* ===== 智能体列表 ===== */
.agent-section {
  flex-shrink: 0;
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 8px;
}

.agent-item {
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
  .agent-item:hover {
    background: var(--bg-hover);
  }
}

.agent-item.active {
  background: var(--accent-light-bg);
}

/* 禁用的智能体：降低透明度 */
.agent-item.disabled {
  opacity: 0.55;
}

@media (hover: hover) {
  .agent-item.disabled:hover {
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
  .agent-item:hover .drag-handle {
    color: var(--text-secondary);
  }
}

.agent-item.drag-over {
  border-top: 2px solid var(--accent-default);
  border-radius: 0;
}

.agent-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-settings {
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
  .agent-item:hover .agent-settings {
    opacity: 1;
  }
}

/* 触屏设备：设置按钮始终可见 */
@media (hover: none) {
  .agent-settings {
    opacity: 1;
  }
}

@media (hover: hover) {
  .agent-settings:hover {
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
