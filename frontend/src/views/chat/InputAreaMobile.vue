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
  el.style.height = '2.75rem'
  el.style.height = Math.min(el.scrollHeight, 150) + 'px'
}

watch(() => props.modelValue, () => nextTick(() => autoResize()))
onMounted(() => nextTick(() => autoResize()))

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

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
      <div class="input-body-mobile">
        <textarea
            ref="inputRef"
            :value="modelValue"
            class="chat-input-mobile"
            placeholder="输入消息..."
            rows="1"
            enterkeyhint="send"
            @input="onInput"
            @keydown="handleKeydown"
            @compositionstart="onCompositionStart"
            @compositionend="onCompositionEnd"
        />
      </div>

      <!-- 底部工具栏 -->
      <div class="input-toolbar-mobile">
        <button class="toolbar-btn-mobile" title="新对话" @click="chatStore.newConversation()">
          <Plus :size="iconSizeSm" :stroke-width="2"/>
        </button>

        <button class="toolbar-btn-mobile toolbar-btn-label" title="选择模型" @click="showModelSheet = true">
          <span class="model-name-mobile">{{ currentModelName }}</span>
          <ChevronDown :size="14" :stroke-width="2"/>
        </button>

        <div class="toolbar-spacer-mobile"/>

        <button
            class="toolbar-btn-mobile"
            :class="{active: chatStore.thinkingMode !== 'auto'}"
            title="思考模式"
            @click="showThinkingSheet = true"
        >
          <Brain :size="iconSizeSm" :stroke-width="2"/>
        </button>

        <button
            class="toolbar-btn-mobile"
            :class="{active: chatStore.webSearchEnabled}"
            title="联网搜索"
            @click="chatStore.webSearchEnabled = !chatStore.webSearchEnabled"
        >
          <Globe :size="iconSizeSm" :stroke-width="2"/>
        </button>

        <button
            class="toolbar-btn-mobile"
            :class="{active: chatStore.mcpToolMode !== 'auto'}"
            title="MCP 服务"
            @click="showMcpSheet = true"
        >
          <Network :size="iconSizeSm" :stroke-width="2"/>
        </button>

        <button
            v-if="!chatStore.isStreaming"
            class="send-btn-mobile"
            :disabled="!modelValue.trim()"
            title="发送"
            @click="emit('send')"
        >
          <Send :size="iconSizeSm" :stroke-width="2.5"/>
        </button>
        <button
            v-else
            class="stop-btn-mobile"
            title="中断"
            @click="chatStore.abortStream()"
        >
          <Square :size="iconSizeSm" :stroke-width="2.5"/>
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

.input-body-mobile {
  display: flex;
  min-height: var(--space-24);
  max-height: 9.375rem;
  padding: var(--space-3) var(--space-3) 0;
  overflow: hidden;
}

.chat-input-mobile {
  width: 100%;
  min-height: var(--space-24);
  padding: var(--space-1) 0;
  border: none;
  border-radius: 0;
  font-size: max(var(--font-size-body), 16px); /* max 确保 ≥ 16px，阻止 iOS Safari 自动缩放 */
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

.input-toolbar-mobile {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0 var(--space-2) var(--space-2);
  flex-shrink: 0;
}

.toolbar-spacer-mobile {
  flex: 1;
}

.toolbar-btn-mobile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--space-10);
  height: var(--space-10);
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-quaternary);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.toolbar-btn-mobile:active {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.toolbar-btn-mobile.active {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.toolbar-btn-label {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  width: auto;
  padding: 0 var(--space-3);
  white-space: nowrap;
  font-size: var(--font-size-small);
  font-family: inherit;
  color: var(--text-tertiary);
}

.model-name-mobile {
  max-width: 5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.send-btn-mobile,
.stop-btn-mobile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--space-10);
  height: var(--space-10);
  border: none;
  border-radius: 8px;
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
