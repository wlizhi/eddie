<template>
  <aside class="provider-sidebar">
    <div class="sidebar-list" @dragover.prevent @drop="onDrop">
      <div
          v-for="(p, i) in providers"
          :key="p.id"
          class="provider-item"
          :class="{ active: activeId === p.id, 'drag-over': dragOverIdx === i }"
          draggable="true"
          @dragstart="onDragStart(i)"
          @dragover="onDragOver(i)"
          @dragleave="onDragLeave"
          @dragend="onDragEnd"
          @click="$emit('select', p)"
      >
        <div class="provider-info">
          <div class="provider-name" :class="{ disabled: !p.enabled }">{{ p.name || p.code }}</div>
          <div class="provider-code">{{ p.code }}</div>
        </div>
        <label class="sidebar-toggle" @click.stop @click.stop.prevent="toggleEnabled(p)">
          <input
              type="checkbox"
              :checked="p.enabled === 1"
              @click.stop
              @change.stop
          />
          <span class="toggle-track"></span>
        </label>
      </div>
      <div v-if="providers.length === 0 && !loading" class="sidebar-empty">
        暂无服务商
      </div>
      <div v-if="loading" class="sidebar-loading">加载中...</div>
    </div>
    <button class="sidebar-add-btn" @click="$emit('add')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
           stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"/>
        <line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
      新增服务商
    </button>
  </aside>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import type {ModelProvider} from '@/types/modelProvider'

const props = defineProps<{
  providers: ModelProvider[]
  activeId?: number
  loading: boolean
}>()

const emit = defineEmits<{
  select: [p: ModelProvider]
  add: []
  toggle: [p: ModelProvider]
  sort: [orderedIds: number[]]
}>()

// ===== 启用/禁用切换 =====
function toggleEnabled(p: ModelProvider) {
  emit('toggle', p)
}

// ===== 拖拽排序 =====
const dragOverIdx = ref<number | null>(null)
let dragSrcIdx = -1

function onDragStart(idx: number) {
  dragSrcIdx = idx
}

function onDragOver(idx: number) {
  dragOverIdx.value = idx
}

function onDragLeave() {
  dragOverIdx.value = null
}

function onDragEnd() {
  dragOverIdx.value = null
  dragSrcIdx = -1
}

function onDrop() {
  const from = dragSrcIdx
  const to = dragOverIdx.value
  dragOverIdx.value = null
  if (from === -1 || to === null || from === to) return

  // 重新排序 providers 数组
  const list = [...props.providers]
  const [moved] = list.splice(from, 1)
  list.splice(to, 0, moved)
  // 发出排序后的 id 列表
  emit('sort', list.map(p => p.id))
}
</script>

<style scoped src="./model-provider.css"></style>
