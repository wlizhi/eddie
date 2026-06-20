<!--
  AssistantDialog.vue — 助手设置编辑弹窗

  功能：
  - 编辑助手名称、头像、描述、System Prompt
  - 切换模型（复用模型选择器数据）
  - 设置记忆轮数
  - 保存到后端
-->
<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {NModal, NSelect} from 'naive-ui'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {fetchAssistantDetail} from '@/api/assistant'
import type {AssistantDetailVO} from '@/types/assistant'

const props = defineProps<{
  /** 要编辑的助手 ID，null 时弹窗不显示 */
  assistantId: number | null
}>()

const emit = defineEmits<{
  'update:assistantId': [value: number | null]
}>()

const assistantStore = useAssistantStore()
const chatStore = useChatStore()

/** 当前编辑的助手详情 */
const detail = ref<AssistantDetailVO | null>(null)
const saving = ref(false)
const feedback = ref('') // 保存结果提示

// ========== 表单数据 ==========
const formName = ref('')
const formAvatar = ref('')
const formDescription = ref('')
const formSystemPrompt = ref('')
const formProviderId = ref<number | null>(null)
const formModelId = ref('')
const formMemoryRounds = ref(20)
const formEnabled = ref<number>(1)
// 模型参数
const formTemperature = ref<number | null>(null)
const formMaxTokens = ref<number | null>(null)
const formTopP = ref<number | null>(null)
const formFrequencyPenalty = ref<number | null>(null)
const formPresencePenalty = ref<number | null>(null)

/** 弹窗是否可见 */
const show = ref(false)

watch(() => props.assistantId, async (id) => {
  if (id === null) {
    show.value = false
    return
  }
  feedback.value = ''
  // 先确保模型列表已加载，再加载详情，最后显示弹窗
  if (chatStore.modelSelectors.length === 0) {
    await chatStore.loadModels()
  }
  await loadDetail(id)
  show.value = true
})

/** 加载助手详情并填充表单 */
async function loadDetail(id: number) {
  try {
    const d = await fetchAssistantDetail(id)
    detail.value = d
    formName.value = d.name
    formAvatar.value = d.avatar ?? ''
    formDescription.value = d.description ?? ''
    formSystemPrompt.value = d.systemPrompt ?? ''
    formProviderId.value = d.providerId
    formModelId.value = d.modelId
    formMemoryRounds.value = d.memoryRounds ?? 20
    // enabled: 详情接口返回 boolean(true/false)，统一转 number(1/0)
    formEnabled.value = d.enabled === true || d.enabled === 1 ? 1 : 0
    // 填充模型参数（modelParams 可能为 null 或 {}）
    const mp = d.modelParams || {}
    formTemperature.value = mp.temperature ?? null
    formMaxTokens.value = mp.maxTokens ?? null
    formTopP.value = mp.topP ?? null
    formFrequencyPenalty.value = mp.frequencyPenalty ?? null
    formPresencePenalty.value = mp.presencePenalty ?? null
  } catch (err) {
    feedback.value = '加载助手详情失败'
    console.error(err)
    setTimeout(() => close(), 1500)
  }
}

/** 按供应商分组的模型选项（复用 chatStore 数据） */
const groupedModelOptions = computed(() =>
    chatStore.modelSelectors.map((s) => ({
      type: 'group' as const,
      label: s.providerName,
      key: s.providerCode,
      children: s.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: m.modelId, // 直接用 modelId 作为值
      })),
    }))
)

function onModelSelect(modelId: string | null) {
  if (!modelId) return
  formModelId.value = modelId
  // 从模型列表中查找对应的 providerId
  for (const sel of chatStore.modelSelectors) {
    const found = sel.models.find(m => m.modelId === modelId)
    if (found) {
      formProviderId.value = found.providerId
      return
    }
  }
}

/** 保存 */
async function handleSave() {
  if (!detail.value) return
  saving.value = true
  try {
    // 构建 modelParams（只传有值的参数）
    const modelParams: Record<string, unknown> = {}
    if (formTemperature.value !== null) modelParams.temperature = formTemperature.value
    if (formMaxTokens.value !== null) modelParams.maxTokens = formMaxTokens.value
    if (formTopP.value !== null) modelParams.topP = formTopP.value
    if (formFrequencyPenalty.value !== null) modelParams.frequencyPenalty = formFrequencyPenalty.value
    if (formPresencePenalty.value !== null) modelParams.presencePenalty = formPresencePenalty.value

    await assistantStore.update(detail.value.id, {
      name: formName.value,
      avatar: formAvatar.value || undefined,
      description: formDescription.value || undefined,
      systemPrompt: formSystemPrompt.value || undefined,
      providerId: formProviderId.value ?? undefined,
      modelId: formModelId.value || undefined,
      memoryRounds: formMemoryRounds.value,
      enabled: formEnabled.value,
      modelParams: Object.keys(modelParams).length > 0 ? modelParams : undefined,
    })
    feedback.value = '✅ 保存成功'
    close()
  } catch (err) {
    feedback.value = '❌ 保存失败'
    console.error(err)
  } finally {
    saving.value = false
  }
}

/** 关闭 */
function close() {
  show.value = false
  emit('update:assistantId', null)
}

// 模型列表由 ChatView 统一加载，dialog 只需在打开时用 watch 兜底
</script>

<template>
  <NModal
      :show="show"
      :on-update:show="(v: boolean) => { if (!v) close() }"
      title="助手设置"
      preset="card"
      style="max-width: 520px; width: 90%;"
      :mask-closable="false"
  >
    <template #header>
      <span style="font-weight: 600; font-size: 15px;">⚙️ 助手设置</span>
    </template>

    <div class="form">
      <!-- 名称 -->
      <div class="field">
        <label class="label">名称</label>
        <input v-model="formName" class="input" placeholder="助手名称"/>
      </div>

      <!-- 头像 -->
      <div class="field">
        <label class="label">头像</label>
        <input v-model="formAvatar" class="input" placeholder="emoji (如 🤖) 或图片 URL"/>
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
        <label class="label">模型</label>
        <NSelect
            :value="formModelId || null"
            :options="groupedModelOptions"
            placeholder="选择模型"
            :consistent-menu-width="false"
            @update:value="onModelSelect"
        />
      </div>

      <!-- 记忆轮数 -->
      <div class="field">
        <label class="label">记忆轮数</label>
        <input v-model.number="formMemoryRounds" type="number" class="input" min="1" max="100" style="width: 100px;"/>
      </div>

      <!-- 模型参数 -->
      <div class="field">
        <label class="label">模型参数</label>
        <div class="params-grid">
          <div class="param-item">
            <span class="param-label">Temperature</span>
            <input v-model.number="formTemperature" type="number" step="0.1" min="0" max="2" class="input param-input"
                   placeholder="0.7"/>
          </div>
          <div class="param-item">
            <span class="param-label">Max Tokens</span>
            <input v-model.number="formMaxTokens" type="number" step="1" min="1" class="input param-input"
                   placeholder="2048"/>
          </div>
          <div class="param-item">
            <span class="param-label">Top P</span>
            <input v-model.number="formTopP" type="number" step="0.1" min="0" max="1" class="input param-input"
                   placeholder="0.9"/>
          </div>
          <div class="param-item">
            <span class="param-label">Frequency Penalty</span>
            <input v-model.number="formFrequencyPenalty" type="number" step="0.1" min="-2" max="2"
                   class="input param-input" placeholder="0"/>
          </div>
          <div class="param-item">
            <span class="param-label">Presence Penalty</span>
            <input v-model.number="formPresencePenalty" type="number" step="0.1" min="-2" max="2"
                   class="input param-input" placeholder="0"/>
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

    <template #footer>
      <div class="footer">
        <span v-if="feedback" class="feedback">{{ feedback }}</span>
        <button class="btn btn-cancel" @click="close">取消</button>
        <button class="btn btn-save" :disabled="saving || !!feedback" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
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
</style>
