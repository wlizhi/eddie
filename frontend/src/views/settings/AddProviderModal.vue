<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-content">
        <div class="modal-header">
          <h3>新增服务商</h3>
          <button class="modal-close" @click="$emit('close')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 服务商 Code -->
          <div class="modal-field">
            <span class="field-label">Code <span class="required">*</span></span>
            <input
                v-model="form.code"
                class="field-input"
                :class="{ 'field-error': errors.code }"
                placeholder="唯一标识，如 custom-openai"
                @input="errors.code = ''"
            />
            <span v-if="errors.code" class="field-err-msg">{{ errors.code }}</span>
          </div>

          <!-- 服务商名称 -->
          <div class="modal-field">
            <span class="field-label">名称 <span class="required">*</span></span>
            <input
                v-model="form.name"
                class="field-input"
                :class="{ 'field-error': errors.name }"
                placeholder="显示名称，如 自定义 OpenAI"
                @input="errors.name = ''"
            />
            <span v-if="errors.name" class="field-err-msg">{{ errors.name }}</span>
          </div>

          <!-- API 地址 -->
          <div class="modal-field">
            <span class="field-label">Base URL <span class="required">*</span></span>
            <input
                v-model="form.baseUrl"
                class="field-input"
                :class="{ 'field-error': errors.baseUrl }"
                placeholder="https://api.openai.com/v1"
                @input="errors.baseUrl = ''"
            />
            <span v-if="errors.baseUrl" class="field-err-msg">{{ errors.baseUrl }}</span>
          </div>

          <!-- API 密钥 -->
          <div class="modal-field">
            <span class="field-label">API Key</span>
            <input
                v-model="form.apiKey"
                class="field-input"
                type="password"
                placeholder="可选，可在后续修改"
            />
          </div>

          <!-- 启用状态 -->
          <div class="modal-field">
            <span class="field-label">状态</span>
            <label class="toggle-row">
              <input type="checkbox" v-model="form.enabled" true-value="1" false-value="0"/>
              <span class="toggle-switch"></span>
              <span class="toggle-label">{{ form.enabled === 1 ? '启用' : '禁用' }}</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-save" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue'
import {createProvider} from '@/api/modelProvider'

const emit = defineEmits<{
  close: []
  created: []
}>()

defineProps<{
  visible: boolean
}>()

const submitting = ref(false)

const form = reactive({
  code: '',
  name: '',
  baseUrl: '',
  apiKey: '',
  enabled: 1 as 0 | 1,
})

const errors = reactive({
  code: '',
  name: '',
  baseUrl: '',
})

function validate(): boolean {
  let valid = true

  if (!form.code.trim()) {
    errors.code = '请输入服务商 code'
    valid = false
  } else if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(form.code.trim())) {
    errors.code = 'code 须以字母开头，仅支持字母、数字、下划线和连字符'
    valid = false
  }

  if (!form.name.trim()) {
    errors.name = '请输入服务商名称'
    valid = false
  }

  if (!form.baseUrl.trim()) {
    errors.baseUrl = '请输入 API 地址'
    valid = false
  } else {
    try {
      new URL(form.baseUrl.trim())
    } catch {
      errors.baseUrl = 'API 地址格式不正确，须以 http:// 或 https:// 开头'
      valid = false
    }
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  submitting.value = true
  try {
    await createProvider({
      code: form.code.trim(),
      name: form.name.trim(),
      baseUrl: form.baseUrl.trim().replace(/\/+$/, ''),
      apiKey: form.apiKey.trim() || undefined,
      enabled: form.enabled,
    })
    emit('created')
  } catch (e) {
    console.error('新增服务商失败', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped src="./model-provider.css"></style>
