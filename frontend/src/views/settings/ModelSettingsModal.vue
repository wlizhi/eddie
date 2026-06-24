<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-content">
        <div class="modal-header">
          <h3>模型设置</h3>
          <button class="modal-close" @click="$emit('close')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 模型 ID（只读） -->
          <div class="modal-field">
            <span class="field-label">模型 ID</span>
            <input class="field-input" :value="form.code" disabled/>
          </div>

          <!-- 模型名称（可自定义） -->
          <div class="modal-field">
            <span class="field-label">模型名称</span>
            <input class="field-input" v-model="form.name" placeholder="自定义名称，留空则使用模型 ID"/>
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
              <input
                  v-if="form.currency === 'custom'"
                  v-model="form.customCurrency"
                  class="currency-custom-input"
                  maxlength="1"
                  placeholder="¥"
              />
            </div>
          </div>

          <!-- 价格 -->
          <div class="modal-field">
            <span class="field-label">输入价格</span>
            <div class="price-row">
              <input
                  v-model.number="form.inputPrice"
                  class="field-input"
                  type="number"
                  min="0"
                  step="0.001"
                  placeholder="0"
              />
              <span class="price-unit">{{ displayCurrency }}/百万Token</span>
            </div>
          </div>
          <div class="modal-field">
            <span class="field-label">输出价格</span>
            <div class="price-row">
              <input
                  v-model.number="form.outputPrice"
                  class="field-input"
                  type="number"
                  min="0"
                  step="0.001"
                  placeholder="0"
              />
              <span class="price-unit">{{ displayCurrency }}/百万Token</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-save" @click="handleSave">保存</button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
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
  })
}
</script>

<style scoped src="./model-provider.css"></style>
