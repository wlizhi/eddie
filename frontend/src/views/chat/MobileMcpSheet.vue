<!--
 * @author Eddie
 * @date 2026-06-28
-->

<!--
  MobileMcpSheet.vue — 移动端 MCP 工具模式 BottomSheet
-->
<script setup lang="ts">
import {ref} from 'vue'
import {useChatStore} from '@/stores/chat'
import {NButton, NCheckbox, NCheckboxGroup, NModal, NSpace} from 'naive-ui'

const chatStore = useChatStore()

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const toolModeOptions = [
  {label: '禁用', value: 'disabled'},
  {label: '自动', value: 'auto'},
  {label: '手动', value: 'manual'},
]

const mcpTempSelected = ref<number[]>([])

function onOpen() {
  mcpTempSelected.value = [...chatStore.selectedMcpServerIds]
}

function onToolModeChange(val: string) {
  chatStore.mcpToolMode = val as 'disabled' | 'auto' | 'manual'
  if (val === 'manual') {
    mcpTempSelected.value = [...chatStore.selectedMcpServerIds]
  }
}

function confirmSelection() {
  chatStore.selectedMcpServerIds = mcpTempSelected.value
  emit('update:show', false)
}
</script>

<template>
  <NModal
      :show="show"
      preset="card"
      title="MCP 服务"
      :style="{ width: '90vw', maxWidth: '24rem', borderRadius: '16px 16px 0 0' }"
      :segmented="{ footer: true }"
      :mask-closable="true"
      transform-origin="center"
      @update:show="emit('update:show', $event)"
      @after-enter="onOpen"
  >
    <div class="mobile-sheet-body">
      <!-- 模式选择行 -->
      <div class="mode-option-row">
        <button
            v-for="opt in toolModeOptions"
            :key="opt.value"
            class="option-btn-mobile option-btn-sm"
            :class="{selected: opt.value === chatStore.mcpToolMode}"
            @click="onToolModeChange(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <!-- 手动模式：MCP Server 勾选列表 -->
      <template v-if="chatStore.mcpToolMode === 'manual'">
        <div class="sheet-divider"/>
        <NCheckboxGroup v-model:value="mcpTempSelected">
          <NSpace vertical>
            <div v-for="mcp in chatStore.boundMcpTools" :key="mcp.mcpServerId" class="mcp-checkbox-item-mobile">
              <NCheckbox :value="mcp.mcpServerId" :label="mcp.mcpServerName"/>
              <span class="mcp-tool-count-mobile">{{ mcp.tools.length }} 工具</span>
            </div>
            <div v-if="chatStore.boundMcpTools.length === 0" class="sheet-empty-hint">暂无可用 MCP 服务</div>
          </NSpace>
        </NCheckboxGroup>
      </template>
    </div>
    <template v-if="chatStore.mcpToolMode === 'manual'" #footer>
      <div class="mcp-modal-footer-mobile">
        <span class="mcp-modal-hint-mobile">选择后将启用其下所有工具</span>
        <NButton type="primary" size="small" @click="confirmSelection">确定</NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.mobile-sheet-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: var(--space-2) 0;
}

.sheet-empty-hint {
  padding: var(--space-8) 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--font-size-small);
}

.sheet-divider {
  height: 1px;
  background: var(--border-light);
  margin: var(--space-3) 0;
}

.mode-option-row {
  display: flex;
  gap: var(--space-2);
  padding: 0 var(--space-1);
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

.option-btn-sm {
  flex: 1;
  text-align: center;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-small);
}

.mcp-checkbox-item-mobile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-radius: 8px;
  background: var(--bg-secondary);
  transition: background 0.15s;
}

.mcp-checkbox-item-mobile:active {
  background: var(--bg-hover);
}

.mcp-tool-count-mobile {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}

.mcp-modal-footer-mobile {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mcp-modal-hint-mobile {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}
</style>
