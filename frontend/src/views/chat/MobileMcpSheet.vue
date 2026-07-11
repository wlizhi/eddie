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
import {NButton, NCheckbox, NModal, NSpace} from 'naive-ui'
import {ChevronDown, ChevronRight} from '@lucide/vue'
import type {ToolSourceVO} from '@/types/mcpServer'

const chatStore = useChatStore()

defineProps<{ show: boolean }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const toolModeOptions = [
  {label: '禁用', value: 'disabled'},
  {label: '自动', value: 'auto'},
  {label: '手动', value: 'manual'},
]

/** 临时勾选的工具名列表（确认后才写入 store） */
const mcpTempSelectedToolNames = ref<string[]>([])

/** MCP 服务展开状态 */
const expandedMcpServers = ref<Record<number, boolean>>({})

function toggleMcpExpand(mcpId: number) {
  expandedMcpServers.value[mcpId] = !expandedMcpServers.value[mcpId]
}

function isMcpExpanded(mcpId: number): boolean {
  return !!expandedMcpServers.value[mcpId]
}

function isMcpServerChecked(mcp: ToolSourceVO): boolean {
  return mcp.tools.some(t => mcpTempSelectedToolNames.value.includes(t.name))
}

function isMcpServerIndeterminate(mcp: ToolSourceVO): boolean {
  const selected = mcp.tools.filter(t => mcpTempSelectedToolNames.value.includes(t.name))
  return selected.length > 0 && selected.length < mcp.tools.length
}

function onMcpServerCheck(checked: boolean, mcp: ToolSourceVO) {
  if (checked) {
    const existing = new Set(mcpTempSelectedToolNames.value)
    for (const tool of mcp.tools) {
      existing.add(tool.name)
    }
    mcpTempSelectedToolNames.value = [...existing]
  } else {
    const toolNames = new Set(mcp.tools.map(t => t.name))
    mcpTempSelectedToolNames.value = mcpTempSelectedToolNames.value.filter(n => !toolNames.has(n))
  }
}

function onToolCheck(checked: boolean, toolName: string) {
  if (checked) {
    mcpTempSelectedToolNames.value.push(toolName)
  } else {
    mcpTempSelectedToolNames.value = mcpTempSelectedToolNames.value.filter(n => n !== toolName)
  }
}

function getToolStatusLabel(mcp: ToolSourceVO, toolName: string): string {
  const tool = mcp.tools.find(t => t.name === toolName)
  if (!tool) return ''
  if (tool.enabledStatus === 1) return '自动'
  if (tool.enabledStatus === 2) return '审批'
  return '禁用'
}

function onOpen() {
  mcpTempSelectedToolNames.value = [...chatStore.selectedToolNames]
}

function onToolModeChange(val: string) {
  chatStore.mcpToolMode = val as 'disabled' | 'auto' | 'manual'
  if (val === 'manual') {
    mcpTempSelectedToolNames.value = [...chatStore.selectedToolNames]
  }
}

function confirmSelection() {
  chatStore.selectedToolNames = mcpTempSelectedToolNames.value
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

      <!-- 手动模式：MCP 工具树形选择 -->
      <template v-if="chatStore.mcpToolMode === 'manual'">
        <div class="sheet-divider"/>
        <NSpace vertical>
          <div v-for="mcp in chatStore.boundMcpTools" :key="mcp.mcpServerId" class="mcp-tree-item-mobile">
            <!-- 服务级行 -->
            <div class="mcp-server-row-mobile" @click="toggleMcpExpand(mcp.mcpServerId)">
              <span class="mcp-expand-icon-mobile">
                <ChevronRight v-if="!isMcpExpanded(mcp.mcpServerId)" :size="14"/>
                <ChevronDown v-else :size="14"/>
              </span>
              <span class="mcp-server-checkbox-wrap-mobile" @click.stop>
                <NCheckbox
                    :checked="isMcpServerChecked(mcp)"
                    :indeterminate="isMcpServerIndeterminate(mcp)"
                    @update:checked="(v: boolean) => onMcpServerCheck(v, mcp)"
                >
                  {{ mcp.mcpServerName }}
                </NCheckbox>
              </span>
              <span class="mcp-tool-count-mobile">{{ mcp.tools.length }} 工具</span>
            </div>
            <!-- 工具级列表 -->
            <div v-if="isMcpExpanded(mcp.mcpServerId)" class="mcp-tool-list-mobile">
              <div v-for="tool in mcp.tools" :key="tool.name" class="mcp-tool-row-mobile">
                <NCheckbox
                    :checked="mcpTempSelectedToolNames.includes(tool.name)"
                    @update:checked="(v: boolean) => onToolCheck(v, tool.name)"
                >
                  {{ tool.displayName || tool.name }}
                </NCheckbox>
                <span class="mcp-tool-status-mobile" :class="getToolStatusLabel(mcp, tool.name) === '自动' ? 'status-auto-mobile' : 'status-approval-mobile'">{{ getToolStatusLabel(mcp, tool.name) }}</span>
              </div>
            </div>
          </div>
          <div v-if="chatStore.boundMcpTools.length === 0" class="sheet-empty-hint">暂无可用 MCP 服务</div>
        </NSpace>
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

.mcp-tree-item-mobile {
  border-radius: 8px;
  background: var(--bg-secondary);
  overflow: hidden;
}

.mcp-server-row-mobile {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}

.mcp-server-row-mobile:active {
  background: var(--bg-hover);
}

.mcp-expand-icon-mobile {
  display: flex;
  align-items: center;
  color: var(--text-tertiary);
  flex-shrink: 0;
  width: var(--space-5);
  justify-content: center;
}

.mcp-server-checkbox-wrap-mobile {
  flex: 1;
  min-width: 0;
}

.mcp-tool-count-mobile {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.mcp-tool-list-mobile {
  border-top: 1px solid var(--border-light);
  padding: var(--space-2) 0;
}

.mcp-tool-row-mobile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-4) var(--space-2) var(--space-12);
  transition: background 0.15s;
}

.mcp-tool-row-mobile:active {
  background: var(--bg-hover);
}

.mcp-tool-status-mobile {
  font-size: var(--font-size-small);
  padding: 0 var(--space-3);
  border-radius: 4px;
  line-height: 1.6;
  flex-shrink: 0;
}

.mcp-tool-status-mobile.status-auto-mobile {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.mcp-tool-status-mobile.status-approval-mobile {
  color: var(--warning-default, #d97706);
  background: var(--warning-light-bg, #fef3c7);
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
