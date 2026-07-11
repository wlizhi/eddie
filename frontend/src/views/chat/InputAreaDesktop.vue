<!--
 * @author Eddie
 * @date 2026-06-21
-->

<!--
  InputAreaDesktop.vue — 桌面端底部输入区域

  功能：
  - 自适应高度的文本输入框（最大 300px，超出滚动）
  - 顶部拖拽手柄自由调整高度（48px~300px）
  - Enter 发送 / Shift+Enter 换行
  - IME 输入法组合处理
  - 发送/中断按钮（工具栏最右侧）
  - 模型选择器（单 NSelect 下拉，与助手弹窗风格一致）
  - 功能开关（联网搜索、深度思考，预留）
  - 流式响应状态提示
-->
<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {NButton, NCheckbox, NModal, NPopselect, NSelect, NSpace} from 'naive-ui'
import {Brain, ChevronDown, ChevronRight, Globe, Network, Plus, Send, Square} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import type {ToolSourceVO} from '@/types/mcpServer'

const {iconSizeXs, iconSizeSm} = useIconSize()

const chatStore = useChatStore()

const props = defineProps<{
  modelValue: string
}>()

/** MCP 工具模式选项 */
const toolModeOptions = [
  {label: '禁用', value: 'disabled'},
  {label: '自动', value: 'auto'},
  {label: '手动', value: 'manual'},
]

/** 当前工具模式的显示标签 */
const toolModeLabel = computed(() => {
  const selected = toolModeOptions.find(o => o.value === chatStore.mcpToolMode)
  return `MCP · ${selected?.label || '自动'}`
})

/** 手动模式 MCP 选择弹窗 */
const showMcpModal = ref(false)

/** 打开手动模式 MCP 选择弹窗 */
function openMcpSelector() {
  showMcpModal.value = true
}

/** 确认 MCP 选择 */
function confirmMcpSelection() {
  showMcpModal.value = false
}

/** MCP 服务展开状态 */
const expandedMcpServers = ref<Record<number, boolean>>({})

/** 切换 MCP 服务展开/折叠 */
function toggleMcpExpand(mcpId: number) {
  expandedMcpServers.value[mcpId] = !expandedMcpServers.value[mcpId]
}

/** MCP 服务是否已展开 */
function isMcpExpanded(mcpId: number): boolean {
  return !!expandedMcpServers.value[mcpId]
}

/** MCP 服务是否有任一工具被选中 */
function isMcpServerChecked(mcp: ToolSourceVO): boolean {
  return mcp.tools.some(t => chatStore.selectedToolNames.includes(t.name))
}

/** MCP 服务是否部分选中（半选状态） */
function isMcpServerIndeterminate(mcp: ToolSourceVO): boolean {
  const selected = mcp.tools.filter(t => chatStore.selectedToolNames.includes(t.name))
  return selected.length > 0 && selected.length < mcp.tools.length
}

/** 点击 MCP 服务 checkbox：全选/全取消 */
function onMcpServerCheck(checked: boolean, mcp: ToolSourceVO) {
  if (checked) {
    // 全选：将服务的所有工具名加入 selectedToolNames（去重）
    const existing = new Set(chatStore.selectedToolNames)
    for (const tool of mcp.tools) {
      existing.add(tool.name)
    }
    chatStore.selectedToolNames = [...existing]
  } else {
    // 全取消：从 selectedToolNames 中移除该服务的所有工具名
    const toolNames = new Set(mcp.tools.map(t => t.name))
    chatStore.selectedToolNames = chatStore.selectedToolNames.filter(n => !toolNames.has(n))
  }
}

/** 点击工具 checkbox：单选/取消 */
function onToolCheck(checked: boolean, toolName: string) {
  if (checked) {
    chatStore.selectedToolNames.push(toolName)
  } else {
    chatStore.selectedToolNames = chatStore.selectedToolNames.filter(n => n !== toolName)
  }
}

/** 工具状态标签 */
function getToolStatusLabel(mcp: ToolSourceVO, toolName: string): string {
  const tool = mcp.tools.find(t => t.name === toolName)
  if (!tool) return ''
  if (tool.enabledStatus === 1) return '自动'
  if (tool.enabledStatus === 2) return '审批'
  return '禁用'
}

/** 工具模式切换时，如果切到手动则弹出 MCP 选择器 */
function onToolModeChange(val: string) {
  chatStore.mcpToolMode = val as 'disabled' | 'auto' | 'manual'
  if (val === 'manual') {
    openMcpSelector()
  }
}

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
}>()

/** textarea DOM 引用 */
const inputRef = ref<HTMLTextAreaElement | null>(null)
/** IME 输入法组合状态 */
const isComposing = ref(false)
/** 拖拽设定的高度（48~220），拖拽时直接生效，不受内容撑高覆盖 */
const baseHeight = ref(48)
/** 输入框高度限制 */
const INPUT_MIN = 48
const INPUT_MAX = 220

/** 内容变化时自动撑高（受 baseHeight 下限和 INPUT_MAX 上限约束），同时同步 baseHeight */
function autoResize() {
  const el = inputRef.value
  if (!el) return
  const contentTarget = Math.min(el.scrollHeight, INPUT_MAX)
  const target = Math.max(baseHeight.value, contentTarget)
  el.style.height = target + 'px'
  baseHeight.value = target
}

/** 拖拽时直接设高度，不受内容撑高逻辑干扰 */
function applyDragHeight(h: number) {
  const el = inputRef.value
  if (el) el.style.height = h + 'px'
}

// 输入内容变化时自动调整高度（自动撑高，不覆盖拖拽下限）
watch(() => props.modelValue, () => {
  nextTick(() => autoResize())
})

onMounted(() => {
  nextTick(() => autoResize())
})

/** 顶部拖拽手柄 —— 以 textarea 实际当前高度为起点，保证拖拽始终跟随鼠标 */
function startResize(e: MouseEvent) {
  e.preventDefault()
  let startY = e.clientY
  let startHeight = inputRef.value?.offsetHeight ?? baseHeight.value

  // 缓存工具栏元素和原始位置
  const toolbarEl = document.querySelector('.input-toolbar') as HTMLElement | null
  const toolbarOriginalBottom = toolbarEl?.getBoundingClientRect().bottom ?? 0

  // 记录最近一次验证安全的高度
  let safeHeight = startHeight

  function onMouseMove(e: MouseEvent) {
    const delta = startY - e.clientY // 向上拖为正（放大），向下拖为负（缩小）
    const candidate = Math.min(INPUT_MAX, Math.max(INPUT_MIN, startHeight + delta))

    // 先应用高度
    applyDragHeight(candidate)

    // 检测工具栏是否向下偏移 —— 只要下移了立刻回退
    const toolbarCurrentBottom = toolbarEl?.getBoundingClientRect().bottom ?? 0
    if (toolbarCurrentBottom > toolbarOriginalBottom) {
      // 工具栏下移了：回退 + 重置起始点，避免下次又尝试大高度
      applyDragHeight(safeHeight)
      baseHeight.value = safeHeight
      startY = e.clientY
      startHeight = safeHeight
    } else {
      // 安全，更新基准
      safeHeight = candidate
      baseHeight.value = candidate
    }
  }

  function onMouseUp() {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

/** 复合键分隔符 */
const MODEL_KEY_SEPARATOR = '::'

/** 按供应商分组 */
const groupedOptions = computed(() =>
    chatStore.modelSelectors.map((selector) => ({
      type: 'group' as const,
      label: selector.providerName,
      key: selector.providerCode,
      children: selector.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: `${m.providerId}${MODEL_KEY_SEPARATOR}${m.modelId}`,
      })),
    }))
)

/** 当前选中项的复合键 */
const selectedModelKey = computed<string | null>(() => {
  if (!chatStore.currentProviderId || !chatStore.currentModelId) return null
  return `${chatStore.currentProviderId}${MODEL_KEY_SEPARATOR}${chatStore.currentModelId}`
})

/** 模型选择 */
function onModelSelect(compositeKey: string | null) {
  if (!compositeKey) return
  const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
  if (sepIdx === -1) return
  const providerId = Number(compositeKey.substring(0, sepIdx))
  const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
  if (!providerId || !modelId) return
  chatStore.selectModel(modelId, providerId)
}

/** 键盘事件 */
function handleKeydown(e: KeyboardEvent) {
  if (e.isComposing || e.keyCode === 229) return
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    emit('send')
  }
}

function onCompositionStart() {
  isComposing.value = true
}

function onCompositionEnd() {
  isComposing.value = false
}

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

/** 思考模式选项（下拉菜单与按钮标签的单一数据源） */
const thinkingModeOptions = [
  {label: '自动', value: 'auto'},
  {label: '精简', value: 'low'},
  {label: '均衡', value: 'medium'},
  {label: '深入', value: 'high'},
  {label: '穷举', value: 'max'},
  {label: '关闭', value: 'disabled'},
]

/** 当前思考模式的显示标签：思考 · 选中项 */
const thinkingModeLabel = computed(() => {
  const selected = thinkingModeOptions.find(o => o.value === chatStore.thinkingMode)
  return `思考 · ${selected?.label || '自动'}`
})

function focusInput() {
  inputRef.value?.focus()
}

defineExpose({focusInput})
</script>

<template>
  <div class="input-area">
    <div class="input-container">
      <div class="input-body">
        <!-- 顶部拖拽手柄 -->
        <div class="resize-handle" @mousedown="startResize"/>
        <div class="grow-wrap">
          <textarea
              ref="inputRef"
              :value="modelValue"
              class="chat-input"
              placeholder="输入消息..."
              rows="1"
              @input="onInput"
              @keydown="handleKeydown"
              @compositionstart="onCompositionStart"
              @compositionend="onCompositionEnd"
          />
        </div>
      </div>

      <!-- 底部工具栏（flex 列布局，固定在底部） -->
      <div class="input-toolbar">
        <button class="new-chat-btn" title="新对话" @click="chatStore.newConversation()">
          <Plus :size="iconSizeSm" :stroke-width="2"/>
        </button>
        <NSelect
            :value="selectedModelKey"
            :options="groupedOptions"
            size="tiny"
            class="model-select"
            placeholder="选择模型"
            filterable
            :consistent-menu-width="false"
            @update:value="onModelSelect"
        />
        <div class="feature-toggles">
          <!-- 思考模式 -->
          <NPopselect
              :value="chatStore.thinkingMode"
              :options="thinkingModeOptions"
              size="small"
              trigger="click"
              @update:value="(val: string) => chatStore.thinkingMode = val"
          >
            <button
                class="toggle-chip"
                :class="{active: chatStore.thinkingMode !== 'auto'}"
                title="思考模式"
            >
              <Brain :size="iconSizeXs" :stroke-width="2"/>
              {{ thinkingModeLabel }}
            </button>
          </NPopselect>

          <!-- 🌐 联网搜索 toggle -->
          <!-- Rule 1: 无已启用的联网工具时置灰不可点击 -->
          <button
              class="toggle-chip"
              :class="{active: chatStore.webSearchEnabled, disabled: !chatStore.canWebSearch}"
              :disabled="!chatStore.canWebSearch"
              title="联网搜索"
              @click="chatStore.canWebSearch && (chatStore.webSearchEnabled = !chatStore.webSearchEnabled)"
          >
            <Globe :size="iconSizeSm" :stroke-width="2"/>
            联网
          </button>

          <!-- 🛠️ MCP 工具模式 -->
          <NPopselect
              :value="chatStore.mcpToolMode"
              :options="toolModeOptions"
              size="small"
              trigger="click"
              @update:value="onToolModeChange"
          >
            <button
                class="toggle-chip"
                :class="{active: chatStore.mcpToolMode !== 'auto'}"
                title="MCP 服务"
            >
              <Network :size="iconSizeXs" :stroke-width="2"/>
              {{ toolModeLabel }}
            </button>
          </NPopselect>
        </div>

        <!-- 手动模式 MCP 选择弹窗 -->
        <NModal v-model:show="showMcpModal" title="选择 MCP 服务" preset="card" style="width:420px">
          <NSpace vertical>
            <div v-for="mcp in chatStore.boundMcpTools" :key="mcp.mcpServerId" class="mcp-tree-item">
              <!-- 服务级行 -->
              <div class="mcp-server-row" @click="toggleMcpExpand(mcp.mcpServerId)">
                <span class="mcp-expand-icon">
                  <ChevronRight v-if="!isMcpExpanded(mcp.mcpServerId)" :size="14"/>
                  <ChevronDown v-else :size="14"/>
                </span>
                <span class="mcp-server-checkbox-wrap" @click.stop>
                  <NCheckbox
                      :checked="isMcpServerChecked(mcp)"
                      :indeterminate="isMcpServerIndeterminate(mcp)"
                      @update:checked="(v: boolean) => onMcpServerCheck(v, mcp)"
                  >
                    {{ mcp.mcpServerName }}
                  </NCheckbox>
                </span>
                <span class="mcp-tool-count">{{ mcp.tools.length }} 个工具</span>
              </div>
              <!-- 工具级列表 -->
              <div v-if="isMcpExpanded(mcp.mcpServerId)" class="mcp-tool-list">
                <div v-for="tool in mcp.tools" :key="tool.name" class="mcp-tool-row">
                  <NCheckbox
                      :checked="chatStore.selectedToolNames.includes(tool.name)"
                      @update:checked="(v: boolean) => onToolCheck(v, tool.name)"
                  >
                    {{ tool.displayName || tool.name }}
                  </NCheckbox>
                  <span class="mcp-tool-status" :class="getToolStatusLabel(mcp, tool.name) === '自动' ? 'status-auto' : 'status-approval'">{{ getToolStatusLabel(mcp, tool.name) }}</span>
                </div>
              </div>
            </div>
            <div v-if="chatStore.boundMcpTools.length === 0" class="mcp-empty-hint">暂无可用 MCP 服务</div>
          </NSpace>
          <template #footer>
            <div class="mcp-modal-footer">
              <span class="mcp-modal-hint">勾选具体工具启用于本次对话</span>
              <n-button type="primary" size="small" @click="confirmMcpSelection">确定</n-button>
            </div>
          </template>
        </NModal>
        <button
            v-if="!chatStore.isStreaming"
            class="send-btn"
            :disabled="!modelValue.trim()"
            title="发送"
            @click="emit('send')"
        >
          <Send :size="iconSizeSm" :stroke-width="2.5"/>
        </button>
        <button
            v-else
            class="stop-btn"
            title="中断"
            @click="chatStore.abortStream()"
        >
          <Square :size="iconSizeXs" :stroke-width="2.5"/>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-area {
  flex-shrink: 0;
  padding: var(--space-5) var(--space-8) var(--space-4);
}

/* ===== 输入容器（flex 列布局，工具栏固定在底部） ===== */
.input-container {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-light);
  border-radius: 18px;
  background: var(--bg-secondary);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.input-container:focus-within {
  border-color: var(--accent-default);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px var(--accent-ring);
}

/* ===== 顶部拖拽手柄 ===== */
.resize-handle {
  height: var(--space-2);
  cursor: ns-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.resize-handle::after {
  content: '';
  width: var(--space-16);
  height: .1875rem;
  border-radius: 2px;
  background: var(--border-default);
  transition: background 0.15s, width 0.15s;
}

.input-container:hover .resize-handle,
.resize-handle:hover {
  opacity: 1;
}

.resize-handle:hover::after {
  background: var(--accent-default);
  width: var(--space-20);
}

/* ===== 输入框主体 ===== */
.input-body {
  display: flex;
  flex-direction: column;
  min-height: var(--space-24);
  max-height: 15rem;
  padding: var(--space-5) var(--space-5) 0;
  overflow: hidden;
}

/* 文本区容器 */
.grow-wrap {
  flex: 1;
  min-height: 0;
}

.chat-input {
  width: 100%;
  padding: var(--space-2) 0;
  border: none;
  border-radius: 0;
  font-size: var(--font-size-body);
  font-family: inherit;
  line-height: 1.5;
  resize: none;
  overflow-y: auto;
  outline: none;
  color: var(--text-primary);
  background: transparent;
}

.chat-input::placeholder {
  color: var(--text-tertiary);
}

/* ===== 底部工具栏（固定在底部，不被挤压） ===== */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-5) var(--space-3);
  flex-shrink: 0;
  flex-wrap: wrap;
  row-gap: var(--space-2);
}

/* ===== 模型选择器 ===== */
.model-select {
  width: auto;
  min-width: 0;
  flex-shrink: 0;
}

.model-select :deep(.n-base-selection) {
  border: none !important;
  background: transparent !important;
  border-radius: 8px !important;
  min-height: auto !important;
  padding: 0 var(--space-2);
  transition: background 0.15s;
}

.model-select :deep(.n-base-selection:hover) {
  background: var(--bg-hover) !important;
}

.model-select :deep(.n-base-selection-label) {
  min-height: auto !important;
  height: auto !important;
  padding: 0 var(--space-1);
}

.model-select :deep(.n-base-selection-input) {
  font-size: var(--font-size-small) !important;
  color: var(--text-quaternary) !important;
  height: auto !important;
  min-height: auto !important;
  padding: 2px 1.05em 2px 2px !important;
}

.model-select :deep(.n-base-selection-placeholder) {
  font-size: var(--font-size-small) !important;
  color: var(--text-quaternary) !important;
}

.model-select :deep(.n-base-selection-arrow) {
  font-size: var(--font-size-small) !important;
  color: var(--text-tertiary) !important;
}

.model-select :deep(.n-base-selection-tags) {
  min-height: auto !important;
  padding: 0 !important;
}

.model-select :deep(.n-base-selection .n-base-suffix) {
  min-height: auto !important;
}

/* ===== 功能开关 ===== */
.feature-toggles {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.toggle-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-4);
  font-size: var(--font-size-small);
  font-family: inherit;
  line-height: 1.4;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-quaternary);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.toggle-chip:hover:not(.disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.toggle-chip.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-chip.disabled:hover {
  background: transparent;
}

.toggle-chip.active {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.toggle-chip.active:hover {
  background: var(--accent-light-bg);
}

.toggle-chip:disabled,
.toggle-chip.disabled {
  opacity: 0.35;
  cursor: not-allowed;
  transform: none;
}

/* ===== 发送 / 中断按钮（工具栏最右） ===== */
.send-btn,
.stop-btn {
  margin-left: auto;
  width: var(--size-btn-md);
  height: var(--size-btn-md);
  border: none;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  flex-shrink: 0;
}

.send-btn {
  background: var(--accent-default);
  color: var(--text-inverse);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: var(--accent-hover);
}

.stop-btn {
  background: var(--danger-default);
  color: var(--text-inverse);
  animation: stop-btn-pulse 1.2s ease-in-out infinite;
}

.stop-btn:hover {
  background: var(--danger-hover);
}

@keyframes stop-btn-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 var(--danger-ring);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 0 0 4px var(--danger-ring);
    transform: scale(1.08);
  }
}

/* ===== 新对话按钮（模型选择器左侧） ===== */
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.8em;
  height: 1.8em;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-quaternary);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.new-chat-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

/* ===== MCP 手动模式选择弹窗（树形） ===== */
.mcp-tree-item {
  border-radius: 8px;
  background: var(--bg-secondary);
  overflow: hidden;
}

.mcp-server-row {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}

.mcp-server-row:hover {
  background: var(--bg-hover);
}

.mcp-expand-icon {
  display: flex;
  align-items: center;
  color: var(--text-tertiary);
  flex-shrink: 0;
  width: var(--space-5);
  justify-content: center;
}

.mcp-server-checkbox-wrap {
  flex: 1;
  min-width: 0;
}

.mcp-tool-count {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.mcp-tool-list {
  border-top: 1px solid var(--border-light);
  padding: var(--space-2) 0;
}

.mcp-tool-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-4) var(--space-2) var(--space-12);
  transition: background 0.15s;
}

.mcp-tool-row:hover {
  background: var(--bg-hover);
}

.mcp-tool-status {
  font-size: var(--font-size-small);
  padding: 0 var(--space-3);
  border-radius: 4px;
  line-height: 1.6;
  flex-shrink: 0;
}

.mcp-tool-status.status-auto {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.mcp-tool-status.status-approval {
  color: var(--warning-default, #d97706);
  background: var(--warning-light-bg, #fef3c7);
}

.mcp-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mcp-modal-hint {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}

.mcp-empty-hint {
  padding: var(--space-12) 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--font-size-small);
}
</style>

<style>
.n-base-select-menu {
  border-radius: 12px !important;
  overflow: hidden;
}

.n-base-select-menu .n-scrollbar {
  max-height: 480px !important;
}
</style>
