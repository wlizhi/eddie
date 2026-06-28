<!--
  MobileThinkingSheet.vue — 移动端思考模式选择 BottomSheet
-->
<script setup lang="ts">
import {useChatStore} from '@/stores/chat'
import {NModal} from 'naive-ui'

const chatStore = useChatStore()

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const thinkingModeOptions = [
  {label: '自动', value: 'auto'},
  {label: '精简', value: 'low'},
  {label: '均衡', value: 'medium'},
  {label: '深入', value: 'high'},
  {label: '穷举', value: 'max'},
  {label: '关闭', value: 'disabled'},
]

function select(val: string) {
  chatStore.thinkingMode = val
  emit('update:show', false)
}
</script>

<template>
  <NModal
      :show="show"
      preset="card"
      title="思考模式"
      :style="{ width: '90vw', maxWidth: '24rem', borderRadius: '16px 16px 0 0' }"
      :mask-closable="true"
      transform-origin="center"
      @update:show="emit('update:show', $event)"
  >
    <div class="mobile-sheet-body">
      <button
          v-for="opt in thinkingModeOptions"
          :key="opt.value"
          class="option-btn-mobile"
          :class="{selected: opt.value === chatStore.thinkingMode}"
          @click="select(opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>
  </NModal>
</template>

<style scoped>
.mobile-sheet-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: var(--space-2) 0;
}

.option-btn-mobile {
  display: block;
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: var(--font-size-body);
  font-family: inherit;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.option-btn-mobile:active {
  background: var(--bg-hover);
}

.option-btn-mobile.selected {
  background: var(--accent-light-bg);
  color: var(--accent-default);
  font-weight: 500;
}
</style>
