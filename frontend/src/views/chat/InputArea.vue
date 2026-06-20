<!--
  InputArea.vue — 底部输入区域

  功能：
  - 自适应高度的文本输入框
  - Enter 发送 / Shift+Enter 换行
  - IME 输入法组合处理
  - 发送/中断按钮
  - 模型选择器（胶囊按钮 + 更多下拉）
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

/** 按供应商分组 — naive-ui NSelect 分组格式 */
const groupedOptions = computed(() =>
    chatStore.modelSelectors.map((selector) => ({
      type: 'group' as const,
      label: selector.providerName,
      key: selector.providerCode,
      children: selector.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: m.modelId,
      })),
    }))
)

/** 当前选中供应商的名称 */
const currentProviderName = computed(() => {
  const sel = chatStore.modelSelectors.find(
      s => s.providerId === chatStore.currentProviderId,
  )
  return sel?.providerName ?? ''
})

/** 当前供应商下的模型列表 */
const currentProviderModels = computed(() => {
  const sel = chatStore.modelSelectors.find(
      s => s.providerId === chatStore.currentProviderId,
  )
  return sel?.models ?? []
})

const QUICK_MODEL_LIMIT = 3

/** 快速切换胶囊模型 */
const quickModels = computed(() =>
    currentProviderModels.value.slice(0, QUICK_MODEL_LIMIT)
)

/** 是否有更多模型需要下拉选择 */
const hasMoreModels = computed(() =>
    currentProviderModels.value.length > QUICK_MODEL_LIMIT
)

/** NSelect "更多" 下拉选中时同步 providerId */
function onModelSelect(modelId: string) {
  for (const sel of chatStore.modelSelectors) {
    const found = sel.models.find(m => m.modelId === modelId)
    if (found) {
      chatStore.selectModel(found.modelId, found.providerId)
      return
    }
  }
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
    <!-- 输入框 + 按钮 -->
    <div class="input-row">
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

    <!-- 底部栏 -->
    <div class="bottom-bar">
      <div class="model-selector-area">
        <span v-if="currentProviderName" class="provider-tag">{{ currentProviderName }}</span>

        <button
            v-for="m in quickModels"
            :key="m.modelId"
            class="model-chip"
            :class="{ active: m.modelId === chatStore.currentModelId }"
            @click="chatStore.selectModel(m.modelId, m.providerId)"
        >
          {{ m.displayName ?? m.modelId }}
        </button>

        <NSelect
            v-if="hasMoreModels"
            :value="chatStore.currentModelId"
            :options="groupedOptions"
            size="tiny"
            class="more-select"
            placeholder="更多 ▾"
            :show-arrow="false"
            @update:value="onModelSelect"
        />
      </div>

      <div class="feature-toggles">
        <button class="toggle-chip disabled" title="联网搜索（即将上线）">
          🌐 联网
        </button>
        <button class="toggle-chip disabled" title="深度思考（即将上线）">
          💡 深度思考
        </button>
      </div>

      <span v-if="chatStore.isStreaming" class="streaming-hint">正在生成...</span>
    </div>
  </div>
</template>

<style scoped>
.input-area {
  flex-shrink: 0;
  border-top: 1px solid #e6e8ec;
  padding: 10px 16px 12px;
  background: #fff;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e0e2e6;
  border-radius: 8px;
  font-size: 13.5px;
  font-family: inherit;
  line-height: 1.5;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s;
  color: #1f1f1f;
  background: #fafbfc;
}

.chat-input:focus {
  border-color: #2563eb;
  background: #fff;
}

.send-btn,
.stop-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  flex-shrink: 0;
}

.send-btn {
  background: #2563eb;
  color: #fff;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: #1d4ed8;
}

.stop-btn {
  background: #ef4444;
  color: #fff;
}

.stop-btn:hover {
  background: #dc2626;
}

/* ===== 底部栏 ===== */
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding: 0 2px;
  gap: 8px;
}

.model-selector-area {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.provider-tag {
  font-size: 11px;
  font-weight: 500;
  color: #9ca3af;
  background: #f0f1f3;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  user-select: none;
}

.model-chip {
  padding: 3px 10px;
  font-size: 12px;
  font-family: inherit;
  line-height: 1.4;
  border: 1px solid #e0e2e6;
  border-radius: 14px;
  background: #fafbfc;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.model-chip:hover {
  background: #f0f1f3;
  border-color: #c4c7cc;
}

.model-chip.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.more-select {
  min-width: 70px;
  max-width: 110px;
}

.feature-toggles {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.toggle-chip {
  padding: 3px 10px;
  font-size: 12px;
  font-family: inherit;
  line-height: 1.4;
  border: 1px solid #e0e2e6;
  border-radius: 14px;
  background: #fafbfc;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.toggle-chip:hover {
  background: #f0f1f3;
  border-color: #c4c7cc;
}

.toggle-chip.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-chip.disabled:hover {
  background: #fafbfc;
  border-color: #e0e2e6;
}

.streaming-hint {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
}
</style>
