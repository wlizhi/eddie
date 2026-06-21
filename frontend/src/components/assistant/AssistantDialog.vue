<!--
  AssistantDialog.vue — 助手设置编辑弹窗

  功能：
  - 编辑助手名称、头像、描述、System Prompt
  - 切换模型（复用模型选择器数据）
  - 设置记忆轮数
  - 保存到后端
-->
<script setup lang="ts">
import {NModal, NSelect, NTooltip} from 'naive-ui'
import {useAssistantForm} from '@/composables/useAssistantForm'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'
import {TIP_THEME_OVERRIDES} from '@/constants/theme'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AvatarPicker from '../common/AvatarPicker.vue'

const props = defineProps<{
  assistantId: number | null
  createVisible?: boolean
}>()

const emit = defineEmits<{
  'update:assistantId': [value: number | null]
  'update:createVisible': [value: boolean]
}>()

const {
  show, saving, feedback, fieldErrors, isCreateMode,
  formName, formAvatar, formDescription, formSystemPrompt,
  formModelId, formMemoryRounds, formEnabled,
  formModelParams, showPicker, originalAvatar,
  clearFieldError, onModelSelect, onAvatarPicked,
  handleSave, handleDelete, close,
  groupedModelOptions,
} = useAssistantForm(props, emit)

const modelParamDefs = MODEL_PARAM_DEFS
const tipTheme = TIP_THEME_OVERRIDES
</script>

<template>
  <NModal
      :show="show"
      :on-update:show="(v: boolean) => { if (!v) close() }"
      title="助手设置"
      preset="card"
      style="max-width: 600px; width: 90%; max-height: 92vh;"
      :mask-closable="false"
  >
    <template #header>
      <span style="font-weight: 600; font-size: 15px;">{{ isCreateMode ? '✨ 新建助手' : '⚙️ 助手设置' }}</span>
    </template>

    <div class="form">
      <!-- 头像（顶部居中，点击编辑） -->
      <div class="avatar-section">
        <div class="avatar-wrap" @click="showPicker = true" title="点击修改头像">
          <AssistantAvatar :name="formName || '?'" :avatar="formAvatar" :size="88"/>
          <div class="avatar-overlay">编辑</div>
        </div>
      </div>

      <!-- 名称 -->
      <div class="field">
        <label class="label">
          名称 <span class="required-star">*</span>
        </label>
        <input
            v-model="formName"
            :class="['input', { 'input-error': fieldErrors['formName'] }]"
            placeholder="助手名称"
            @input="clearFieldError('formName')"
        />
        <span v-if="fieldErrors['formName']" class="field-error-msg">{{ fieldErrors['formName'] }}</span>
      </div>

      <!-- 描述 -->
      <div class="field">
        <label class="label">描述</label>
        <input v-model="formDescription" class="input" placeholder="简短描述"/>
      </div>

      <!-- System Prompt -->
      <div class="field">
        <label class="label">System Prompt</label>
        <textarea v-model="formSystemPrompt" class="textarea" rows="4" placeholder="系统提示词"/>
      </div>

      <!-- 模型选择 -->
      <div class="field">
        <label class="label">
          模型 <span class="required-star">*</span>
        </label>
        <div :class="['nselect-error-wrap', { 'nselect-error': fieldErrors['formModelId'] }]">
          <NSelect
              :value="formModelId || null"
              :options="groupedModelOptions"
              placeholder="选择模型"
              :consistent-menu-width="false"
              @update:value="(v: string | null) => { onModelSelect(v); clearFieldError('formModelId') }"
          />
        </div>
        <span v-if="fieldErrors['formModelId']" class="field-error-msg">{{ fieldErrors['formModelId'] }}</span>
      </div>

      <!-- 记忆轮数 -->
      <div class="field">
        <label class="label">
          记忆轮数
          <NTooltip trigger="hover" placement="top" :theme-overrides="tipTheme" :show-arrow="false">
            <template #trigger>
              <span class="hint-icon">ⓘ</span>
            </template>
            AI 能记住的对话轮数，越大记忆越久但也更耗 tokens。推荐 10~30
          </NTooltip>
        </label>
        <input v-model.number="formMemoryRounds" type="number" class="input" min="1" max="100" style="width: 100px;"/>
      </div>

      <!-- 模型参数 -->
      <div class="field">
        <label class="label">模型参数</label>
        <div class="params-grid">
          <div class="param-item" v-for="def in modelParamDefs" :key="def.key">
            <span class="param-label">
              {{ def.label }}
              <NTooltip trigger="hover" placement="top" :theme-overrides="tipTheme" :show-arrow="false">
                <template #trigger>
                  <span class="hint-icon">ⓘ</span>
                </template>
                {{ def.tip }}
              </NTooltip>
            </span>
            <input v-model.number="formModelParams[def.key]" type="number"
                   :step="def.step" :min="def.min" :max="def.max" class="input param-input"/>
          </div>
        </div>
      </div>

      <!-- 启用/禁用（放最后） -->
      <div class="field">
        <label class="label">状态</label>
        <label class="toggle-row">
          <input v-model.number="formEnabled" type="checkbox" :true-value="1" :false-value="0" class="toggle-input"/>
          <span class="toggle-label">{{ formEnabled === 1 ? '启用' : '禁用' }}</span>
        </label>
      </div>
    </div>

    <!-- 头像选择弹窗 -->
    <NModal :show="showPicker" preset="card" title="选择头像"
            style="max-width: 420px; width: 90%;"
            :mask-closable="false"
            @update:show="(v: boolean) => { if (!v) showPicker = false }">
      <AvatarPicker :current-avatar="originalAvatar"
                    @confirm="onAvatarPicked"
                    @close="showPicker = false"/>
    </NModal>

    <template #footer>
      <div class="footer">
        <button v-if="!isCreateMode" class="btn btn-delete" @click="handleDelete">🗑 删除</button>
        <span v-if="feedback" class="feedback">{{ feedback }}</span>
        <button class="btn btn-cancel" @click="close">取消</button>
        <button class="btn btn-save" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
/* ===== 头像区域（顶部居中） ===== */
.avatar-section {
  display: flex;
  justify-content: center;
  padding: 6px 0 10px;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}

.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}

/* 弹窗表单：内容超出时仅表单区域内部滚动，不撑出屏幕 */
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(92vh - 140px);
  overflow-y: auto;
  /* 用负 margin 穿透 card-content 的 padding，
     让滚动条贴到卡片右边缘，不压内容 */
  margin: 0 -24px;
  padding: 4px 24px;
  /* 细滚动条（Chrome/Safari） */
  scrollbar-width: thin;
}

.form::-webkit-scrollbar {
  width: 5px;
}

.form::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.form::-webkit-scrollbar-track {
  background: transparent;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
}

.input {
  padding: 7px 10px;
  border: 1px solid #e0e2e6;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
  color: #1f1f1f;
  background: #fff;
}

.input:focus {
  border-color: #2563eb;
}

.textarea {
  padding: 7px 10px;
  border: 1px solid #e0e2e6;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
  color: #1f1f1f;
  background: #fff;
  resize: vertical;
  min-height: 80px;
  line-height: 1.5;
}

.textarea:focus {
  border-color: #2563eb;
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn {
  padding: 7px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  border: 1px solid transparent;
}

.btn-cancel {
  background: #f4f5f7;
  color: #6b7280;
  border-color: #e0e2e6;
}

.btn-cancel:hover {
  background: #e8eaee;
}

.btn-save {
  background: #2563eb;
  color: #fff;
}

.btn-save:hover {
  background: #1d4ed8;
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.feedback {
  font-size: 13px;
  margin-right: auto;
  color: #6b7280;
}

/* ===== 启用/禁用开关 ===== */
.toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.toggle-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.toggle-label {
  font-size: 13px;
  color: #1f1f1f;
}

/* ===== 模型参数网格 ===== */
.params-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.param-label {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}

.param-input {
  width: 100%;
}

/* ===== 提示图标 ===== */
.hint-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 13px;
  height: 13px;
  font-size: 10px;
  color: #a0aec0;
  cursor: pointer;
  margin-left: 3px;
  vertical-align: middle;
  line-height: 1;
  transition: color 0.15s;
}

.hint-icon:hover {
  color: #4a5568;
}

.btn-delete {
  background: transparent;
  color: #e74c3c;
  border-color: #f5c6cb;
  margin-right: auto;
  transition: background 0.15s;
}

.btn-delete:hover {
  background: #fef2f2;
}

/* ===== 必填标记 ===== */
.required-star {
  color: #ef4444;
  margin-left: 2px;
  font-weight: 700;
}

/* ===== 字段错误状态 ===== */
.input-error {
  border-color: #ef4444 !important;
  animation: error-pulse 1.2s ease-in-out infinite;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
}

@keyframes error-pulse {
  0%, 100% {
    border-color: #ef4444;
    box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
  }
  50% {
    border-color: #dc2626;
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25);
  }
}

.field-error-msg {
  font-size: 11px;
  color: #ef4444;
  margin-top: 2px;
  line-height: 1.3;
}

/* ===== NSelect 错误边框包裹 ===== */
.nselect-error-wrap {
  border-radius: 6px;
  transition: box-shadow 0.15s;
}

.nselect-error-wrap.nselect-error {
  border-radius: 6px;
  animation: nselect-error-pulse 1.2s ease-in-out infinite;
}

.nselect-error-wrap.nselect-error :deep(.n-base-selection) {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
}

@keyframes nselect-error-pulse {
  0%, 100% {
    box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.28);
  }
}

/* ===== feedback 错误样式（API 失败等场景） ===== */
.feedback-error {
  color: #ef4444 !important;
}

.feedback-success {
  color: #16a34a !important;
}
</style>
