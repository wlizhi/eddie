<!--
  InputArea.vue — 底部输入区域

  功能：
  - 自适应高度的文本输入框（最大 300px，超出滚动）
  - 顶部拖拽手柄自由调整高度（48px~300px）
  - Enter 发送 / Shift+Enter 换行
  - IME 输入法组合处理
  - 发送/中断按钮（工具栏最右侧）
  - 模型选择器（单 NSelect 下拉，与助手弹窗风格一致）
  - 功能开关（联网搜索、深度思考，预留）
  - 流式响应状态提示

  数据来源：
  - modelSelectors, currentModelId, currentProviderId, isStreaming → Pinia store
  - modelValue → 父组件 v-model

  与父组件通信：
  - modelValue (string) — 输入框文本
  - send — 用户点击发送或按 Enter
-->
<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {NPopselect, NSelect} from 'naive-ui'
import {Brain, Globe, Plus, Send, Square} from '@lucide/vue'

const chatStore = useChatStore()

const props = defineProps<{
  modelValue: string
}>()

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
  const startY = e.clientY
  const el = inputRef.value
  const startHeight = el ? el.offsetHeight : baseHeight.value

  function onMouseMove(e: MouseEvent) {
    const delta = startY - e.clientY // 向上拖为正（放大），向下拖为负（缩小）
    const newHeight = Math.max(INPUT_MIN, Math.min(INPUT_MAX, startHeight + delta))
    baseHeight.value = newHeight
    applyDragHeight(newHeight)
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
  {label: '低', value: 'low'},
  {label: '中', value: 'medium'},
  {label: '高', value: 'high'},
  {label: '极限', value: 'max'},
  {label: '禁用', value: 'disabled'},
]

/** 当前思考模式的显示标签（从 thinkingModeOptions 派生） */
const thinkingModeLabel = computed(() => {
  if (chatStore.thinkingMode === 'auto') return '思考'
  return thinkingModeOptions.find(o => o.value === chatStore.thinkingMode)?.label || '思考'
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
          <Plus :size="14" :stroke-width="2"/>
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
              <Brain :size="12" :stroke-width="2"/>
              {{ thinkingModeLabel }}
            </button>
          </NPopselect>
          <button class="toggle-chip disabled" title="联网搜索（即将上线）">
            <Globe :size="13" :stroke-width="2"/>
            联网
          </button>
        </div>
        <button
            v-if="!chatStore.isStreaming"
            class="send-btn"
            :disabled="!modelValue.trim()"
            title="发送"
            @click="emit('send')"
        >
          <Send :size="13" :stroke-width="2.5"/>
        </button>
        <button
            v-else
            class="stop-btn"
            title="中断"
            @click="chatStore.abortStream()"
        >
          <Square :size="11" :stroke-width="2.5"/>
        </button>
      </div>
    </div>
  </div>
</template>

<style src="./input-area.css" scoped/>
<style>
.n-base-select-menu {
  border-radius: 12px !important;
  overflow: hidden;
}

.n-base-select-menu .n-scrollbar {
  max-height: 480px !important;
}
</style>
