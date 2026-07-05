<!--
 * @author Eddie
 * @date 2026-07-04
-->

<!--
  AgentInputArea.vue — 智能体输入区域

  功能：
  - 自适应高度的文本输入框（最大 220px，超出滚动）
  - 顶部拖拽手柄自由调整高度（48px~220px）
  - Enter 发送 / Shift+Enter 换行
  - IME 输入法组合处理
  - 发送/中断按钮（工具栏最右侧）
  - 模型选择器（单 NSelect 下拉，展示当前智能体主模型）
  - 功能开关（联网搜索、思考模式、MCP 工具）
  - MCP 手动模式选择弹窗
-->
<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useAgentChatStore} from '@/stores/agent-chat'
import {useAgentStore} from '@/stores/agent'
import {NButton, NCheckbox, NCheckboxGroup, NModal, NPopselect, NSelect, NSpace} from 'naive-ui'
import {Brain, Globe, Network, Plus, Send, Square} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'

const {iconSizeXs, iconSizeSm} = useIconSize()

const agentChatStore = useAgentChatStore()
const agentStore = useAgentStore()

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
  const selected = toolModeOptions.find(o => o.value === agentChatStore.mcpToolMode)
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

/** 工具模式切换时，如果切到手动则弹出 MCP 选择器 */
function onToolModeChange(val: string) {
  agentChatStore.mcpToolMode = val as 'disabled' | 'auto' | 'manual'
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

/** 按供应商分组的模型选项 */
const groupedOptions = computed(() =>
    agentChatStore.modelSelectors.map((selector) => ({
      type: 'group' as const,
      label: selector.providerName,
      key: selector.providerCode,
      children: selector.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: `${m.providerId}${MODEL_KEY_SEPARATOR}${m.modelId}`,
      })),
    }))
)

/** 当前选中项的复合键（取智能体配置的主模型） */
const selectedModelKey = computed<string | null>(() => {
  const agent = agentStore.activeAgent
  if (!agent?.mainProviderId || !agent?.mainModelId) return null
  return `${agent.mainProviderId}${MODEL_KEY_SEPARATOR}${agent.mainModelId}`
})

/** 模型选择（临时覆盖，仅当前请求生效，刷新回归 Agent 配置） */
function onModelSelect(compositeKey: string | null) {
  if (!compositeKey) {
    agentChatStore.selectedProviderId = null
    agentChatStore.selectedModelId = null
    return
  }
  const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
  if (sepIdx === -1) return
  const providerId = Number(compositeKey.substring(0, sepIdx))
  const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
  if (!providerId || !modelId) return
  agentChatStore.selectedProviderId = providerId
  agentChatStore.selectedModelId = modelId
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
  const selected = thinkingModeOptions.find(o => o.value === agentChatStore.thinkingMode)
  return `思考 · ${selected?.label || '自动'}`
})

function focusInput() {
  inputRef.value?.focus()
}

defineExpose({focusInput})
</script>

<template>
  <div class="agent-input-area">
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

      <!-- 底部工具栏 -->
      <div class="input-toolbar">
        <button class="new-chat-btn" title="新对话" @click="agentChatStore.newConversation()">
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
              :value="agentChatStore.thinkingMode"
              :options="thinkingModeOptions"
              size="small"
              trigger="click"
              @update:value="(val: string) => agentChatStore.thinkingMode = val"
          >
            <button
                class="toggle-chip"
                :class="{active: agentChatStore.thinkingMode !== 'auto'}"
                title="思考模式"
            >
              <Brain :size="iconSizeXs" :stroke-width="2"/>
              {{ thinkingModeLabel }}
            </button>
          </NPopselect>

          <!-- 🌐 联网搜索 toggle -->
          <button
              class="toggle-chip"
              :class="{active: agentChatStore.webSearchEnabled}"
              title="联网搜索"
              @click="agentChatStore.webSearchEnabled = !agentChatStore.webSearchEnabled"
          >
            <Globe :size="iconSizeSm" :stroke-width="2"/>
            联网
          </button>

          <!-- 🛠️ MCP 工具模式 -->
          <NPopselect
              :value="agentChatStore.mcpToolMode"
              :options="toolModeOptions"
              size="small"
              trigger="click"
              @update:value="onToolModeChange"
          >
            <button
                class="toggle-chip"
                :class="{active: agentChatStore.mcpToolMode !== 'auto'}"
                title="MCP 服务"
            >
              <Network :size="iconSizeXs" :stroke-width="2"/>
              {{ toolModeLabel }}
            </button>
          </NPopselect>
        </div>

        <!-- 手动模式 MCP 选择弹窗 -->
        <NModal v-model:show="showMcpModal" title="选择 MCP 服务" preset="card" style="width:420px">
          <NCheckboxGroup v-model:value="agentChatStore.selectedMcpServerIds">
            <NSpace vertical>
              <div v-for="mcp in agentChatStore.boundMcpTools" :key="mcp.mcpServerId" class="mcp-checkbox-item">
                <NCheckbox :value="mcp.mcpServerId" :label="mcp.mcpServerName"/>
                <span class="mcp-tool-count">{{ mcp.tools.length }} 个工具</span>
              </div>
              <div v-if="agentChatStore.boundMcpTools.length === 0" class="mcp-empty-hint">暂无可用 MCP 服务</div>
            </NSpace>
          </NCheckboxGroup>
          <template #footer>
            <div class="mcp-modal-footer">
              <span class="mcp-modal-hint">选择 MCP 后将启用其下所有工具</span>
              <n-button type="primary" size="small" @click="confirmMcpSelection">确定</n-button>
            </div>
          </template>
        </NModal>
        <button
            v-if="!agentChatStore.isStreaming"
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
            @click="agentChatStore.abortStream()"
        >
          <Square :size="iconSizeXs" :stroke-width="2.5"/>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.agent-input-area {
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

/* 功能开关区 */
.feature-toggles {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* toggle 按钮 */
.toggle-chip {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: var(--font-size-xs);
  color: var(--text-quaternary);
  white-space: nowrap;
  transition: background 0.15s, color 0.15s;
}

.toggle-chip:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.toggle-chip.active {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

/* 新对话按钮 */
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: var(--text-quaternary);
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
}

.new-chat-btn:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

/* 发送/中断按钮 */
.send-btn,
.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, opacity 0.15s;
  flex-shrink: 0;
  margin-left: auto;
}

.send-btn {
  background: var(--accent-default);
  color: #fff;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
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
</style>
