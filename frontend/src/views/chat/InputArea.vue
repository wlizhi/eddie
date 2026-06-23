<!--
  InputArea.vue — 底部输入区域

  功能：
  - 自适应高度的文本输入框
  - Enter 发送 / Shift+Enter 换行
  - IME 输入法组合处理
  - 发送/中断按钮
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
import {computed, ref} from 'vue'
import {useChatStore} from '@/stores/chat'
import {NSelect} from 'naive-ui'
import {Send, Square} from '@lucide/vue'

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

/** 复合键分隔符，防止不同供应商下同编号模型冲突 */
const MODEL_KEY_SEPARATOR = '::'

/** 按供应商分组 — naive-ui NSelect 分组格式（复合键） */
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

/** NSelect 选中时从复合键解析并同步 store */
function onModelSelect(compositeKey: string | null) {
  if (!compositeKey) return
  const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
  if (sepIdx === -1) return
  const providerId = Number(compositeKey.substring(0, sepIdx))
  const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
  if (!providerId || !modelId) return
  chatStore.selectModel(modelId, providerId)
}

/** 处理输入框键盘事件 */
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

/** textarea 自适应高度 */
function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

/** 输入事件处理：更新 v-model + 自适应高度 */
function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
  autoResize(e)
}

/** 发送后保持焦点在输入框 */
function focusInput() {
  inputRef.value?.focus()
}

defineExpose({focusInput})
</script>

<template>
  <div class="input-area">
    <div class="input-container" :class="{ 'is-streaming': chatStore.isStreaming }">
      <!-- 输入框 + 发送按钮 -->
      <div class="input-body">
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
        <button
            v-if="!chatStore.isStreaming"
            class="send-btn"
            :disabled="!modelValue.trim()"
            title="发送"
            @click="emit('send')"
        >
          <Send :size="16" :stroke-width="2"/>
        </button>
        <button
            v-else
            class="stop-btn"
            title="中断"
            @click="chatStore.abortStream()"
        >
          <Square :size="14" :stroke-width="2"/>
        </button>
      </div>

      <!-- 底部工具栏：模型选择 + 功能开关 -->
      <div class="input-toolbar">
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
          <button class="toggle-chip disabled" title="联网搜索（即将上线）">
            🌐 联网
          </button>
          <button class="toggle-chip disabled" title="深度思考（即将上线）">
            💡 思考
          </button>
        </div>
      </div>
    </div>

    <span v-if="chatStore.isStreaming" class="streaming-hint">正在生成...</span>
  </div>
</template>

<style src="./input-area.css" scoped/>
<style>
/* 覆盖 NSelect 下拉菜单样式 */
.n-base-select-menu {
  border-radius: 12px !important;
  overflow: hidden;
}

.n-base-select-menu .n-scrollbar {
  max-height: 480px !important;
}
</style>
