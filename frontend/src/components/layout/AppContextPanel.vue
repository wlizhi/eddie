<script setup lang="ts">
import {ChevronLeft, ChevronRight} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'

const {iconSizeXs} = useIconSize()

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  toggle: []
}>()
</script>

<template>
  <aside class="context-panel" :class="{ collapsed: !visible }">
    <button
        class="panel-collapse-btn"
        :title="visible ? '折叠侧栏' : '展开侧栏'"
        @click="emit('toggle')"
    >
      <component :is="visible ? ChevronLeft : ChevronRight" :size="iconSizeXs" :stroke-width="2"/>
    </button>
    <div class="panel-body">
      <router-view name="panel"/>
    </div>
  </aside>

  <!-- Mobile panel backdrop (tap to close) -->
  <div
      v-if="visible"
      class="panel-backdrop"
      @click="emit('toggle')"
  />
</template>

<style scoped>
/* ===== Context Panel ===== */
.context-panel {
  width: 16.25rem;
  min-width: 16.25rem;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-default);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, min-width 0.2s ease, opacity 0.15s ease;
  overflow: hidden;
  position: relative;
}

.context-panel.collapsed {
  width: 0;
  min-width: 0;
  opacity: 0;
}

.panel-collapse-btn {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  width: 1.375rem;
  height: 1.375rem;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background 0.15s, color 0.15s;
  z-index: 10;
}

.panel-collapse-btn:hover {
  background: var(--bg-hover);
  color: var(--text-quaternary);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
}

/* ===== 移动端响应式：< 768px ===== */
@media (max-width: 48rem) {
  /* Context Panel → 覆盖层抽屉 */
  .context-panel {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 85vw;
    min-width: 0;
    z-index: 300;
    border-right: 1px solid var(--border-default);
    transition: transform 0.2s ease;
    transform: translateX(0);
  }

  .context-panel.collapsed {
    width: 85vw;
    min-width: 0;
    opacity: 1;
    transform: translateX(-100%);
  }

  .panel-collapse-btn {
    display: none;
  }
}
</style>
