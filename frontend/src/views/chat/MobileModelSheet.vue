<!--
 * @author Eddie
 * @date 2026-06-28
-->

<!--
  MobileModelSheet.vue — 移动端模型选择 BottomSheet
-->
<script setup lang="ts">
import {computed} from 'vue'
import {useChatStore} from '@/stores/chat'
import {NModal} from 'naive-ui'

const chatStore = useChatStore()

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

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

function onModelSelect(compositeKey: string | null) {
  if (!compositeKey) return
  const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
  if (sepIdx === -1) return
  const providerId = Number(compositeKey.substring(0, sepIdx))
  const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
  if (!providerId || !modelId) return
  chatStore.selectModel(modelId, providerId)
  emit('update:show', false)
}
</script>

<template>
  <NModal
      :show="show"
      preset="card"
      title="选择模型"
      :style="{ width: '90vw', maxWidth: '24rem', borderRadius: '16px 16px 0 0' }"
      :mask-closable="true"
      transform-origin="center"
      @update:show="emit('update:show', $event)"
  >
    <div class="mobile-sheet-body">
      <div v-for="group in groupedOptions" :key="group.key" class="model-group-mobile">
        <div class="model-group-label">{{ group.label }}</div>
        <button
            v-for="item in group.children"
            :key="item.value"
            class="model-option-mobile"
            :class="{selected: item.value === selectedModelKey}"
            @click="onModelSelect(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <div v-if="groupedOptions.length === 0" class="sheet-empty-hint">暂无可用模型</div>
    </div>
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

.model-group-mobile {
  margin-bottom: var(--space-3);
}

.model-group-label {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  padding: var(--space-2) var(--space-3);
  font-weight: 500;
}

.model-option-mobile {
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

.model-option-mobile:active {
  background: var(--bg-hover);
}

.model-option-mobile.selected {
  background: var(--accent-light-bg);
  color: var(--accent-default);
  font-weight: 500;
}
</style>
