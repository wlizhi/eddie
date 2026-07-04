<!--
 * @author Eddie
 * @date 2026-06-21
-->

<!--
  AssistantDialog.vue — 助手设置编辑弹窗

  功能：
  - 编辑助手名称、头像、描述、System Prompt
  - 切换模型（复用模型选择器数据）
  - 设置记忆轮数
  - 保存到后端
-->
<script setup lang="ts">
import {NButton, NCheckbox, NCheckboxGroup, NInputNumber, NModal, NSelect, NSwitch, NTooltip} from 'naive-ui'
import {Globe, Network, Trash2} from '@lucide/vue'
import {useAssistantForm} from '@/composables/useAssistantForm'
import {useIconSize} from '@/composables/useIconSize'
import {TIP_THEME_OVERRIDES} from '@/constants/theme'
import {showToast} from '@/composables/useToast'
import {ref} from 'vue'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AvatarPicker from '../common/AvatarPicker.vue'
import ModelParamsInput from '../common/ModelParamsInput.vue'
import PromptVariablePanel from '../common/PromptVariablePanel.vue'

const {iconSizeSm} = useIconSize()

const props = defineProps<{
  assistantId: number | null
  createVisible?: boolean
}>()

const emit = defineEmits<{
  'update:assistantId': [value: number | null]
  'update:createVisible': [value: boolean]
}>()

const {
  show, saving, fieldErrors, isCreateMode,
  formName, formAvatar, formDescription, formSystemPrompt,
  formMemoryRounds, formEnabled,
  formModelParams, formPreferences, showPicker, originalAvatar,
  formEnabledMcpServerIds, mcpServerList,
  clearFieldError, onModelSelect, onAvatarPicked,
  insertVariable, systemPromptRef,
  handleSave, handleDelete, close,
  groupedModelOptions,
  selectedModelKey,
} = useAssistantForm(props, emit)

const tipTheme = TIP_THEME_OVERRIDES
const paramHasError = ref(false)

/** 保存前先校验参数范围 */
function handleSaveWithValidation() {
  if (paramHasError.value) {
    showToast('存在标红的参数超出合法范围，请修正后再保存', 'info')
    return
  }
  handleSave()
}

</script>

<template>
  <NModal
      :show="show"
      :on-update:show="(v: boolean) => { if (!v) close() }"
      title="助手设置"
      preset="card"
      style="max-width: 600px; width: 90%;"
      :mask-closable="false"
  >
    <template #header>
      <span style="font-weight: 600; font-size: var(--font-size-lg);">{{
          isCreateMode ? '✨ 新建助手' : '⚙️ 助手设置'
        }}</span>
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
        <textarea ref="systemPromptRef" v-model="formSystemPrompt" class="textarea" rows="4"
                  placeholder="系统提示词 — 可使用 ${变量名} 嵌入动态内容"/>
        <PromptVariablePanel @insert="insertVariable"/>
      </div>

      <!-- 模型选择 -->
      <div class="field">
        <label class="label">
          模型 <span class="required-star">*</span>
        </label>
        <div :class="['nselect-error-wrap', { 'nselect-error': fieldErrors['formModelId'] }]">
          <NSelect
              :value="selectedModelKey"
              :options="groupedModelOptions"
              placeholder="选择模型"
              filterable
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
        <n-input-number v-model:value="formMemoryRounds" :min="1" :max="100" :step="1" style="width: 100px"/>
      </div>

      <!-- 模型参数（含默认思考模式下拉 + 数字参数，自动校验范围） -->
      <div class="field">
        <label class="label">模型参数</label>
        <ModelParamsInput :params="formModelParams" @update:params="Object.assign(formModelParams, $event)"
                          @error="(e: boolean) => paramHasError = e"/>
      </div>

      <!-- 助手偏好（UI 默认状态） -->
      <div class="field">
        <label class="label">助手偏好</label>
        <div class="preferences-group">
          <div class="pref-row">
            <div class="pref-row-label">
              <Globe :size="iconSizeSm" :stroke-width="2"/>
              <span>联网搜索默认</span>
            </div>
            <NSwitch
                :value="!!formPreferences.webSearchEnabled"
                @update:value="(v: boolean) => formPreferences.webSearchEnabled = v"
            />
          </div>
          <div class="pref-row">
            <div class="pref-row-label">
              <Network :size="iconSizeSm" :stroke-width="2"/>
              <span>MCP 默认模式</span>
            </div>
            <NSelect
                :value="formPreferences.mcpToolMode ?? 'auto'"
                :options="[
                  { label: '禁用', value: 'disabled' },
                  { label: '自动', value: 'auto' },
                  { label: '手动', value: 'manual' },
                ]"
                size="tiny"
                class="mcp-mode-select"
                @update:value="(v: 'disabled' | 'auto' | 'manual') => formPreferences.mcpToolMode = v"
            />
          </div>
        </div>
      </div>

      <!-- MCP 服务选择 -->
      <div v-if="mcpServerList.length > 0" class="field">
        <label class="label">MCP 服务</label>
        <div class="mcp-checkbox-list">
          <NCheckboxGroup v-model:value="formEnabledMcpServerIds">
            <NCheckbox
                v-for="mcp in mcpServerList"
                :key="mcp.id"
                :value="mcp.id"
                :label="mcp.name"
                class="mcp-checkbox-item"
            />
          </NCheckboxGroup>
        </div>
      </div>

      <!-- ===== 启用/禁用开关 — 此按钮应放在页面最下方位置 ===== -->
      <div class="field">
        <label class="label">状态</label>
        <div class="switch-row">
          <NSwitch
              :value="formEnabled === 1"
              @update:value="(v: boolean) => formEnabled = v ? 1 : 0"
          />
          <span class="switch-label">{{ formEnabled === 1 ? '启用' : '禁用' }}</span>
        </div>
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
        <NButton v-if="!isCreateMode" type="error" ghost size="small" @click="handleDelete">
          <template #icon>
            <Trash2 :size="iconSizeSm"/>
          </template>
          删除
        </NButton>
        <div class="footer-right">
          <NButton quaternary @click="close">取消</NButton>
          <NButton type="primary" :loading="saving" @click="handleSaveWithValidation">
            {{ saving ? '保存中...' : '保存' }}
          </NButton>
        </div>
      </div>
    </template>
  </NModal>
</template>

<style src="./assistant-dialog.css" scoped/>
