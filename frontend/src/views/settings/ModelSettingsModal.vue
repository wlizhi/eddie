<!--
 * @author Eddie
 * @date 2026-06-22
-->

<template>
  <NModal
      :show="visible"
      preset="card"
      title="模型设置"
      style="max-width: 520px; width: 90%;"
      :mask-closable="false"
      @update:show="(v: boolean) => { if (!v) $emit('close') }"
  >
    <div class="modal-body">
      <!-- 模型 ID（只读） -->
      <div class="modal-field">
        <span class="field-label">模型 ID</span>
        <n-input :value="form.code" disabled/>
      </div>

      <!-- 模型名称（可自定义） -->
      <div class="modal-field">
        <span class="field-label">模型名称</span>
        <n-input v-model:value="form.name" placeholder="自定义名称，留空则使用模型 ID"/>
      </div>

      <!-- 能力选择 -->
      <div class="modal-field">
        <span class="field-label">支持能力</span>
        <div class="capability-grid">
          <div
              v-for="cap in CAPABILITY_TYPES"
              :key="cap.code"
              class="cap-option"
              :class="{ selected: form.capabilities.includes(cap.code) }"
              :style="getCapStyle(cap.code, form.capabilities.includes(cap.code))"
              @click="toggle(cap.code)"
          >
            <span v-html="capIcon(cap.code, 14)"></span>
            {{ cap.label }}
          </div>
        </div>
      </div>

      <!-- 币种 -->
      <div class="modal-field">
        <span class="field-label">币种</span>
        <div class="currency-selector">
          <button
              class="currency-option"
              :class="{ active: form.currency === '¥' }"
              @click="form.currency = '¥'"
          >¥
          </button>
          <button
              class="currency-option"
              :class="{ active: form.currency === '$' }"
              @click="form.currency = '$'"
          >$
          </button>
          <button
              class="currency-option"
              :class="{ active: form.currency === 'custom' }"
              @click="form.currency = 'custom'"
          >自定义
          </button>
          <n-input
              v-if="form.currency === 'custom'"
              v-model:value="form.customCurrency"
              class="currency-custom-input"
              :maxlength="1"
              placeholder="¥"
          />
        </div>
      </div>

      <!-- 价格 -->
      <div class="modal-field">
        <span class="field-label">输入价格</span>
        <div class="price-row">
          <n-input-number
              v-model:value="form.inputPrice"
              :min="0"
              :step="0.001"
              placeholder="0"
          />
          <span class="price-unit">{{ displayCurrency }}/百万Token</span>
        </div>
      </div>
      <div class="modal-field">
        <span class="field-label">输出价格</span>
        <div class="price-row">
          <n-input-number
              v-model:value="form.outputPrice"
              :min="0"
              :step="0.001"
              placeholder="0"
          />
          <span class="price-unit">{{ displayCurrency }}/百万Token</span>
        </div>
      </div>
      <div class="modal-field">
        <span class="field-label">缓存命中价格</span>
        <div class="price-row">
          <n-input-number
              v-model:value="form.cacheInputPrice"
              :min="0"
              :step="0.001"
              placeholder="留空则使用输入价格"
          />
          <span class="price-unit">{{ displayCurrency }}/百万Token</span>
        </div>
      </div>
      <div class="modal-field">
        <span class="field-label">缓存写入价格</span>
        <div class="price-row">
          <n-input-number
              v-model:value="form.cacheWriteInputPrice"
              :min="0"
              :step="0.001"
              placeholder="留空则使用输入价格"
          />
          <span class="price-unit">{{ displayCurrency }}/百万Token</span>
        </div>
      </div>

      <!-- 调用间隔 -->
      <div class="modal-field">
        <span class="field-label">调用间隔（秒）</span>
        <div class="price-row">
          <n-input-number
              v-model:value="form.callIntervalSec"
              :min="0"
              :step="1"
              placeholder="留空不限制"
              :show-button="false"
          />
          <span class="price-unit">秒（两次调用之间的最小间隔）</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <button class="btn-cancel" @click="$emit('close')">取消</button>
        <button class="btn-save" @click="handleSave">保存</button>
      </div>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import {NInput, NInputNumber, NModal} from 'naive-ui'
import {computed, reactive, watch} from 'vue'
import type {ModelItem} from '@/types/modelProvider'
import {CAPABILITY_TYPES, capIcon, getCapStyle, normalizeCaps, toggleCapability,} from './modelCapabilities'

const props = defineProps<{
  model: ModelItem | null
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [payload: {
    code: string
    name: string
    capabilities: string[]
    currency: string
    customCurrency: string
    inputPrice: number
    outputPrice: number
    cacheInputPrice?: number
    cacheWriteInputPrice?: number
    callIntervalSec?: number
  }]
}>()

const form = reactive({
  code: '',
  name: '',
  capabilities: [] as string[],
  currency: '¥',
  customCurrency: '¥',
  inputPrice: 0,
  outputPrice: 0,
  cacheInputPrice: undefined as number | undefined,
  cacheWriteInputPrice: undefined as number | undefined,
  callIntervalSec: undefined as number | undefined,
})

/** 当前显示的币种符号 */
const displayCurrency = computed(() => {
  if (form.currency === 'custom') return form.customCurrency || '¥'
  return form.currency
})

/** 弹窗打开时同步表单数据 */
watch(() => props.model, (m) => {
  if (!m) return
  form.code = m.code
  form.name = m.code
  form.capabilities = normalizeCaps(m.capabilities)
  form.currency = m.currency || '¥'
  form.customCurrency = m.currency && !['¥', '$'].includes(m.currency) ? m.currency : '¥'
  if (m.currency && !['¥', '$'].includes(m.currency)) {
    form.currency = 'custom'
  }
  form.inputPrice = m.inputPrice ?? 0
  form.outputPrice = m.outputPrice ?? 0
  form.cacheInputPrice = m.cacheInputPrice ?? undefined
  form.cacheWriteInputPrice = m.cacheWriteInputPrice ?? undefined
  form.callIntervalSec = m.callIntervalSec ?? undefined
}, {immediate: true})

function toggle(code: string) {
  form.capabilities = toggleCapability(form.capabilities, code)
}

function handleSave() {
  emit('save', {
    code: form.code,
    name: form.name,
    capabilities: [...form.capabilities],
    currency: form.currency,
    customCurrency: form.customCurrency,
    inputPrice: form.inputPrice,
    outputPrice: form.outputPrice,
    cacheInputPrice: form.cacheInputPrice,
    cacheWriteInputPrice: form.cacheWriteInputPrice,
    callIntervalSec: form.callIntervalSec,
  })
}
</script>

<style scoped>
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.capability-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cap-option {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid var(--border-default);
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  transition: all 0.12s;
}

.cap-option.selected {
  border-color: var(--accent-default);
  background: var(--accent-light-bg);
  color: var(--accent-default);
}

.currency-selector {
  display: flex;
  gap: 6px;
  align-items: center;
}

.currency-option {
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--border-default);
  border-radius: 6px;
  background: var(--bg-secondary);
  font-size: 13px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.12s;
}

.currency-option.active {
  border-color: var(--accent-default);
  background: var(--accent-light-bg);
  color: var(--accent-default);
}

.currency-custom-input {
  width: 70px;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-unit {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.btn-cancel {
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--border-default);
  border-radius: 6px;
  background: var(--bg-primary);
  font-size: 13px;
  color: var(--text-quaternary);
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.btn-cancel:hover {
  border-color: var(--border-hover);
  color: var(--text-primary);
}

.btn-save {
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: var(--accent-default);
  font-size: 13px;
  color: var(--text-inverse);
  cursor: pointer;
  transition: background 0.12s;
}

.btn-save:hover {
  background: var(--accent-hover);
}
</style>
