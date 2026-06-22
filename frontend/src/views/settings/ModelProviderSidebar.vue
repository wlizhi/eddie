<template>
  <aside class="provider-sidebar">
    <div class="sidebar-list">
      <div
          v-for="p in providers"
          :key="p.id"
          class="provider-item"
          :class="{ active: activeId === p.id }"
          @click="$emit('select', p)"
      >
        <div class="provider-name">{{ p.name || p.code }}</div>
        <div class="provider-code">{{ p.code }}</div>
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
import type {ModelProvider} from '@/types/modelProvider'

defineProps<{
  providers: ModelProvider[]
  activeId?: number
  loading: boolean
}>()

defineEmits<{
  select: [p: ModelProvider]
  add: []
}>()
</script>

<style scoped src="./model-provider.css"></style>
