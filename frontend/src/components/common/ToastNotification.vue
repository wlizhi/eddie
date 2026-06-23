<template>
  <Teleport to="body">
    <Transition name="toast-fade">
      <div v-if="state.visible" class="toast-overlay">
        <div class="toast" :class="'toast-' + state.type">
          <span class="toast-icon">{{ iconMap[state.type] }}</span>
          <span class="toast-msg">{{ state.message }}</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {useToast} from '@/composables/useToast'

const {state} = useToast()

const iconMap: Record<string, string> = {
  success: '✓',
  error: '✕',
  info: 'ℹ',
}
</script>

<style scoped>
.toast-overlay {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 10px;
  font-size: var(--font-size-base);
  font-weight: 500;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.18);
  pointer-events: auto;
  white-space: nowrap;
}

.toast-success {
  background: var(--success-light-bg);
  color: var(--success-text);
  border: 1px solid var(--success-default);
}

.toast-error {
  background: var(--danger-light-bg);
  color: var(--danger-default);
  border: 1px solid var(--danger-default);
}

.toast-info {
  background: var(--accent-light-bg);
  color: var(--accent-default);
  border: 1px solid var(--accent-default);
}

.toast-icon {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}

/* 过渡动画 */
.toast-fade-enter-active {
  transition: all 0.25s ease-out;
}

.toast-fade-leave-active {
  transition: all 0.2s ease-in;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateY(-12px);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
