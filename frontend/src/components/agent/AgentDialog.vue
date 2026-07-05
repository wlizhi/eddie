<!--
 * @author Eddie
 * @date 2026-07-04
 -->

<!--
  AgentDialog.vue — 智能体设置编辑弹窗

  功能：
  - 编辑智能体名称、头像、描述、System Prompt
  - 切换主模型（复用模型选择器数据）
  - 可选子代理模型配置
  - 设置执行控制参数（并发度、迭代次数、超时）
  - 设置执行模式 / 工具选择模式
  - 偏好设置（联网搜索、MCP 模式）
  - 保存到后端
-->
<script setup lang="ts">
import {NButton, NCheckbox, NCheckboxGroup, NInputNumber, NModal, NSelect, NSwitch, NTooltip} from 'naive-ui'
import {Globe, Network, Trash2} from '@lucide/vue'
import {useAgentForm} from '@/composables/useAgentForm'
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
  agentId: number | null
  createVisible?: boolean
}>()

const emit = defineEmits<{
  'update:agentId': [value: number | null]
  'update:createVisible': [value: boolean]
}>()

const {
  show, saving, fieldErrors, isCreateMode,
  formName, formAvatar, formDescription, formSystemPrompt,
  formEnabled,
  formMainModelParams, formSubModelParams, formPreferences, showPicker, originalAvatar,
  clearFieldError, onMainModelSelect, onSubModelSelect, onAvatarPicked,
  insertVariable, systemPromptRef,
  handleSave, handleDelete, close,
  groupedModelOptions,
  selectedMainModelKey, selectedSubModelKey,

  formMainProviderId, formMainModelId,
  formSubProviderId, formSubModelId,
  formSemaphore, formMaxIterations, formMaxExecutionTimeSec,
  formExecutionMode, formToolSelectionMode,
  formEnabledMcpServerIds, mcpServerList,
} = useAgentForm(props, emit)

const tipTheme = TIP_THEME_OVERRIDES
const mainParamHasError = ref(false)
const subParamHasError = ref(false)

/** 保存前先校验参数范围 */
function handleSaveWithValidation() {
  if (mainParamHasError.value) {
    showToast('主模型存在标红的参数超出合法范围，请修正后再保存', 'info')
    return
  }
  if (formSubProviderId.value && subParamHasError.value) {
    showToast('子代理模型存在标红的参数超出合法范围，请修正后再保存', 'info')
    return
  }
  handleSave()
}
</script>

<template>
  <NModal
      :show="show"
      :on-update:show="(v: boolean) => { if (!v) close() }"
      title="智能体设置"
      preset="card"
      style="max-width: 620px; width: 90%;"
      :mask-closable="false"
  >
    <template #header>
      <span style="font-weight: 600; font-size: var(--font-size-lg);">{{
          isCreateMode ? '🤖 新建智能体' : '⚙️ 智能体设置'
        }}</span>
    </template>

    <div class="form">
      <!-- ===== 头像（顶部居中，点击编辑） ===== -->
      <div class="avatar-section">
        <div class="avatar-wrap" @click="showPicker = true" title="点击修改头像">
          <AssistantAvatar :name="formName || '?'" :avatar="formAvatar" :size="88"/>
          <div class="avatar-overlay">编辑</div>
        </div>
      </div>

      <!-- ===== 名称 ===== -->
      <div class="field">
        <label class="label">
          名称 <span class="required-star">*</span>
        </label>
        <input
            v-model="formName"
            :class="['input', { 'input-error': fieldErrors['formName'] }]"
            placeholder="智能体名称"
            @input="clearFieldError('formName')"
        />
        <span v-if="fieldErrors['formName']" class="field-error-msg">{{ fieldErrors['formName'] }}</span>
      </div>

      <!-- ===== 描述 ===== -->
      <div class="field">
        <label class="label">描述</label>
        <input v-model="formDescription" class="input" placeholder="简短描述该智能体的功能"/>
      </div>

      <!-- ===== System Prompt ===== -->
      <div class="field">
        <label class="label">System Prompt</label>
        <textarea ref="systemPromptRef" v-model="formSystemPrompt" class="textarea" rows="4"
                  placeholder="任务指令 — 可使用 ${变量名} 嵌入动态内容"/>
        <PromptVariablePanel @insert="insertVariable"/>
      </div>

      <hr class="section-divider"/>

      <!-- ============================== -->
      <!-- ===== 主模型配置 ===== -->
      <!-- ============================== -->
      <div class="field">
        <label class="label" style="font-weight: 600; font-size: var(--font-size-base); color: var(--text-primary);">
          主模型配置 <span class="required-star">*</span>
        </label>
      </div>

      <div class="field">
        <label class="label">
          主模型 <span class="required-star">*</span>
        </label>
        <div :class="['nselect-error-wrap', { 'nselect-error': fieldErrors['formMainModelId'] }]">
          <NSelect
              :value="selectedMainModelKey"
              :options="groupedModelOptions"
              placeholder="选择主模型"
              filterable
              :consistent-menu-width="false"
              @update:value="(v: string | null) => { onMainModelSelect(v); clearFieldError('formMainModelId') }"
          />
        </div>
        <span v-if="fieldErrors['formMainModelId']" class="field-error-msg">{{ fieldErrors['formMainModelId'] }}</span>
      </div>

      <!-- 主模型参数 -->
      <div class="field">
        <label class="label">主模型参数</label>
        <ModelParamsInput :params="formMainModelParams" @update:params="Object.assign(formMainModelParams, $event)"
                          @error="(e: boolean) => mainParamHasError = e"/>
      </div>

      <hr class="section-divider"/>

      <!-- ============================== -->
      <!-- ===== 子代理模型配置（可选） ===== -->
      <!-- ============================== -->
      <div class="sub-model-section">
        <div class="sub-model-header">
          <span>子代理模型配置</span>
          <span style="font-weight: 400; color: var(--text-muted);">（可选）</span>
        </div>

        <div class="field">
          <label class="label">子代理模型</label>
          <NSelect
              :value="selectedSubModelKey"
              :options="groupedModelOptions"
              placeholder="选择子代理模型（留空则不启用）"
              filterable
              clearable
              :consistent-menu-width="false"
              @update:value="onSubModelSelect"
          />
        </div>

        <!-- 子代理模型参数 -->
        <div v-if="formSubProviderId" class="field">
          <label class="label">子代理模型参数</label>
          <ModelParamsInput :params="formSubModelParams" @update:params="Object.assign(formSubModelParams, $event)"
                            @error="(e: boolean) => subParamHasError = e"/>
        </div>
      </div>

      <hr class="section-divider"/>

      <!-- ============================== -->
      <!-- ===== 执行控制 ===== -->
      <!-- ============================== -->
      <div class="field">
        <label class="label" style="font-weight: 600; font-size: var(--font-size-base); color: var(--text-primary);">
          执行控制
        </label>
      </div>

      <div class="exec-params-grid">
        <div class="exec-param-item">
          <label class="exec-param-label">
            并发度
            <NTooltip trigger="hover" placement="top" :theme-overrides="tipTheme" :show-arrow="false">
              <template #trigger>
                <span class="hint-icon">ⓘ</span>
              </template>
              同时执行的子任务数量，越大执行越快但消耗更多资源
            </NTooltip>
          </label>
          <n-input-number v-model:value="formSemaphore" :min="1" :max="20" :step="1" style="width: 100%"/>
        </div>

        <div class="exec-param-item">
          <label class="exec-param-label">
            最大迭代次数
            <NTooltip trigger="hover" placement="top" :theme-overrides="tipTheme" :show-arrow="false">
              <template #trigger>
                <span class="hint-icon">ⓘ</span>
              </template>
              智能体最多执行的循环轮数，防止无限执行
            </NTooltip>
          </label>
          <n-input-number v-model:value="formMaxIterations" :min="1" :max="1000" :step="1" style="width: 100%"/>
        </div>

        <div class="exec-param-item">
          <label class="exec-param-label">
            执行超时（秒）
            <NTooltip trigger="hover" placement="top" :theme-overrides="tipTheme" :show-arrow="false">
              <template #trigger>
                <span class="hint-icon">ⓘ</span>
              </template>
              单次执行的最大等待时间，超时自动终止
            </NTooltip>
          </label>
          <n-input-number v-model:value="formMaxExecutionTimeSec" :min="10" :max="3600" :step="10" style="width: 100%"/>
        </div>

        <div class="exec-param-item">
          <label class="exec-param-label">执行模式</label>
          <NSelect
              :value="formExecutionMode"
              :options="[
                { label: '前台执行', value: 'FOREGROUND' },
                { label: '后台执行', value: 'BACKGROUND' },
              ]"
              @update:value="(v: string) => formExecutionMode = v"
              style="width: 100%"
          />
        </div>
      </div>

      <!-- ===== 工具选择模式 ===== -->
      <div class="field">
        <label class="label">工具选择模式</label>
        <NSelect
            :value="formToolSelectionMode"
            :options="[
              { label: '自动选择', value: 'auto' },
              { label: '手动选择', value: 'manual' },
              { label: '不使用工具', value: 'none' },
            ]"
            @update:value="(v: string) => formToolSelectionMode = v"
        />
      </div>

      <!-- ===== 偏好设置 ===== -->
      <div class="field">
        <label class="label">偏好设置</label>
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

      <!-- ===== MCP 服务选择 ===== -->
      <div class="field">
        <label class="label">MCP 服务</label>
        <div v-if="mcpServerList.length === 0" class="mcp-empty-hint">
          暂无已启用的 MCP 服务，请先在「设置 → MCP 服务」中添加
        </div>
        <div v-else class="mcp-checkbox-list">
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

      <!-- ===== 启用/禁用开关 ===== -->
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

<style src="./agent-dialog.css" scoped/>
