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

<style src="./input-area.css" scoped/>
