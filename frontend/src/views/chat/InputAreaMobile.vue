<!--
  InputAreaMobile.vue — 移动端底部输入区域

  设计要点：
  - textarea 始终可见（不会被任何弹窗覆盖）
  - 工具栏图标按钮触发 BottomSheet 子组件
  - 不依赖 @media 查询
  - 触摸友好的大点击区域（44px+）
-->
<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useChatStore} from '@/stores/chat'
import {Brain, ChevronDown, Globe, Network, Plus, Send, Square} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import MobileModelSheet from './MobileModelSheet.vue'
import MobileThinkingSheet from './MobileThinkingSheet.vue'
import MobileMcpSheet from './MobileMcpSheet.vue'

const {iconSizeSm} = useIconSize()

const chatStore = useChatStore()

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
}>()

/** ===== 文本输入 ===== */
const inputRef = ref<HTMLTextAreaElement | null>(null)
const isComposing = ref(false)

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = '2.25rem'
  el.style.height = Math.min(el.scrollHeight, 150) + 'px'
}

watch(() => props.modelValue, () => nextTick(() => autoResize()))
onMounted(() => nextTick(() => autoResize()))

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

function handleKeydown(e: KeyboardEvent) {
  // 移动端：Enter 换行，发送由右侧按钮触发
  // 桌面端 Ctrl+Enter / Shift+Enter 换行由原生行为处理
  if (e.isComposing || e.keyCode === 229) return
}

function onCompositionStart() {
  isComposing.value = true
}

function onCompositionEnd() {
  isComposing.value = false
}

function focusInput() {
  inputRef.value?.focus()
}

defineExpose({focusInput})

/** ===== 模型选择 ===== */
const MODEL_KEY_SEPARATOR = '::'

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

const selectedModelKey = computed<string | null>(() => {
  if (!chatStore.currentProviderId || !chatStore.currentModelId) return null
  return `${chatStore.currentProviderId}${MODEL_KEY_SEPARATOR}${chatStore.currentModelId}`
})

const currentModelName = computed(() => {
  if (!selectedModelKey.value) return '选择模型'
  for (const group of groupedOptions.value) {
    const found = group.children.find(c => c.value === selectedModelKey.value)
    if (found) return found.label
  }
  return '选择模型'
})

const showModelSheet = ref(false)
const showThinkingSheet = ref(false)
const showMcpSheet = ref(false)
</script>

<template>
  <div class="input-area-mobile">
    <div class="input-container-mobile">
      <!-- 可水平滑动的工具栏行 -->
      <div class="toolbar-scroll-row">
        <div class="toolbar-scroll-content">
          <!-- 新对话放在最左侧 -->
          <button class="toolbar-chip toolbar-chip-label" title="新对话" @click="chatStore.newConversation()">
            <Plus :size="iconSizeSm"/>
            <!--            <span>新对话</span>-->
          </button>

          <button class="toolbar-chip toolbar-chip-label" title="选择模型" @click="showModelSheet = true">
            <span class="model-name-mobile">{{ currentModelName }}</span>
            <ChevronDown :size="12"/>
          </button>

          <button
              class="toolbar-chip"
              :class="{active: chatStore.thinkingMode !== 'auto'}"
              title="思考模式"
              @click="showThinkingSheet = true"
          >
            <Brain :size="iconSizeSm"/>
            <span>思考</span>
          </button>

          <button
              class="toolbar-chip"
              :class="{active: chatStore.webSearchEnabled}"
              title="联网搜索"
              @click="chatStore.webSearchEnabled = !chatStore.webSearchEnabled"
          >
            <Globe :size="iconSizeSm"/>
            <span>联网</span>
          </button>

          <button
              class="toolbar-chip"
              :class="{active: chatStore.mcpToolMode !== 'auto'}"
              title="MCP 服务"
              @click="showMcpSheet = true"
          >
            <Network :size="iconSizeSm"/>
            <span>MCP</span>
          </button>
        </div>
      </div>

      <!-- 输入框 + 发送按钮行 -->
      <div class="input-row-mobile">
        <div class="input-body-mobile">
          <textarea
              ref="inputRef"
              :value="modelValue"
              class="chat-input-mobile"
              placeholder="输入消息..."
              rows="1"
              enterkeyhint="enter"
              @input="onInput"
              @keydown="handleKeydown"
              @compositionstart="onCompositionStart"
              @compositionend="onCompositionEnd"
          />
        </div>

        <button
            v-if="!chatStore.isStreaming"
            class="send-btn-mobile"
            :disabled="!modelValue.trim()"
            title="发送"
            @click="emit('send')"
        >
          <Send :size="iconSizeSm"/>
        </button>
        <button
            v-else
            class="stop-btn-mobile"
            title="中断"
            @click="chatStore.abortStream()"
        >
          <Square :size="iconSizeSm"/>
        </button>
      </div>
    </div>

    <!-- BottomSheet 子组件 -->
    <MobileModelSheet v-model:show="showModelSheet"/>
    <MobileThinkingSheet v-model:show="showThinkingSheet"/>
    <MobileMcpSheet v-model:show="showMcpSheet"/>
  </div>
</template>

<style scoped>
.input-area-mobile {
  flex-shrink: 0;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
  padding: var(--space-2) var(--space-3) var(--space-3);
}

.input-container-mobile {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  border: 1px solid var(--border-light);
  border-radius: 14px;
  background: var(--bg-secondary);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.input-container-mobile:focus-within {
  border-color: var(--accent-default);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px var(--accent-ring);
}

/* ===== 可水平滑动的工具栏行 ===== */
.toolbar-scroll-row {
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding: var(--space-2) var(--space-3) 0;
}

.toolbar-scroll-row::-webkit-scrollbar {
  display: none;
}

.toolbar-scroll-content {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  white-space: nowrap;
}

/* Chip 样式工具栏项 */
.toolbar-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-tertiary);
  font-size: var(--font-size-xs);
  font-family: inherit;
  line-height: 1.4;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}

.toolbar-chip:active {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.toolbar-chip.active {
  color: var(--accent-default);
  background: var(--accent-light-bg);
  border-color: var(--accent-light-border);
}

.toolbar-chip-label {
  padding: var(--space-1) var(--space-2);
  border: none;
  background: transparent;
  color: var(--text-quaternary);
}

.toolbar-chip-label:active {
  background: var(--bg-hover);
}

.model-name-mobile {
  max-width: 8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 输入框 + 发送按钮行 ===== */
.input-row-mobile {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-3) var(--space-2);
}

.input-body-mobile {
  flex: 1;
  min-height: var(--touch-btn-size);
  max-height: 9.375rem;
  overflow: hidden;
}

.chat-input-mobile {
  width: 100%;
  min-height: var(--touch-btn-size);
  padding: var(--space-1) 0;
  border: none;
  border-radius: 0;
  font-size: max(var(--font-size-body), 16px);
  font-family: inherit;
  line-height: 1.5;
  resize: none;
  overflow-y: auto;
  outline: none;
  color: var(--text-primary);
  background: transparent;
}

.chat-input-mobile::placeholder {
  color: var(--text-tertiary);
}

/* ===== 发送/停止按钮（增大触摸目标） ===== */
.send-btn-mobile,
.stop-btn-mobile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--touch-btn-size);
  height: var(--touch-btn-size);
  min-width: var(--touch-btn-size);
  min-height: var(--touch-btn-size);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.send-btn-mobile {
  background: var(--accent-default);
  color: var(--text-inverse);
}

.send-btn-mobile:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn-mobile:not(:disabled):active {
  background: var(--accent-hover);
}

.stop-btn-mobile {
  background: var(--danger-default);
  color: var(--text-inverse);
  animation: stop-btn-pulse 1.2s ease-in-out infinite;
}

.stop-btn-mobile:active {
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
