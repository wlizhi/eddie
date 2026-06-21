<!--
  AssistantDialog.vue — 助手设置编辑弹窗

  功能：
  - 编辑助手名称、头像、描述、System Prompt
  - 切换模型（复用模型选择器数据）
  - 设置记忆轮数
  - 保存到后端
-->
<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue'
import {NModal, NSelect, NTooltip} from 'naive-ui'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {fetchAssistantDetail, updateAssistantAvatar} from '@/api/assistant'
import type {AssistantDetailVO} from '@/types/assistant'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AvatarPicker from '../common/AvatarPicker.vue'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const tipTheme: any = {
  peers: {
    popover: {
      padding: '5px 9px',
      fontSize: '12px',
      borderRadius: '5px',
      color: '#ffffff',
      textColor: '#374151',
      boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
      border: '1px solid #e5e7eb',
    },
  },
}

const props = defineProps<{
  assistantId: number | null
}>()

const emit = defineEmits<{
  'update:assistantId': [value: number | null]
}>()

const assistantStore = useAssistantStore()
const chatStore = useChatStore()

const detail = ref<AssistantDetailVO | null>(null)
const saving = ref(false)
const feedback = ref('')

// ========== 表单数据 ==========
const formName = ref('')
const formAvatar = ref('')
const formDescription = ref('')
const formSystemPrompt = ref('')
const formProviderId = ref<number | null>(null)
const formModelId = ref('')
const formMemoryRounds = ref(20)
const formEnabled = ref<number>(1)

const modelParamDefs = [
  {
    key: 'temperature',
    label: 'Temperature',
    tip: '控制回答的随机性。越高越有创造力，越低越保守准确。范围 0~2，推荐 0.5~1.2',
    step: 0.1,
    min: 0,
    max: 2
  },
  {
    key: 'maxTokens',
    label: 'Max Tokens',
    tip: '单次回答的最大长度。越大可生成越长内容，但更耗资源。推荐 1024~4096',
    step: 1,
    min: 1
  },
  {
    key: 'topP',
    label: 'Top P',
    tip: '候选词筛选阈值。值越小回答越保守，通常配合 Temperature 使用。范围 0~1',
    step: 0.1,
    min: 0,
    max: 1
  },
  {
    key: 'frequencyPenalty',
    label: 'Frequency Penalty',
    tip: '减少词语重复。值越大越避免重复已有词汇。范围 -2~2，推荐 0~1',
    step: 0.1,
    min: -2,
    max: 2
  },
  {
    key: 'presencePenalty',
    label: 'Presence Penalty',
    tip: '鼓励谈论新话题。值越大越倾向讨论不同内容。范围 -2~2，推荐 0~1',
    step: 0.1,
    min: -2,
    max: 2
  },
]
const formModelParams = reactive<Record<string, number | null>>(
    Object.fromEntries(modelParamDefs.map(d => [d.key, null]))
)

const show = ref(false)
const showPicker = ref(false)
const originalAvatar = ref('')
const pendingAvatarFile = ref<File | null>(null)

watch(() => props.assistantId, async (id) => {
  if (id === null) {
    show.value = false
    return
  }
  feedback.value = ''
  if (chatStore.modelSelectors.length === 0) {
    await chatStore.loadModels()
  }
  await loadDetail(id)
  show.value = true
})

async function loadDetail(id: number) {
  try {
    const d = await fetchAssistantDetail(id)
    detail.value = d
    formName.value = d.name
    formAvatar.value = d.avatar ?? ''
    originalAvatar.value = d.avatar ?? ''
    formDescription.value = d.description ?? ''
    formSystemPrompt.value = d.systemPrompt ?? ''
    formProviderId.value = d.providerId
    formModelId.value = d.modelId
    formMemoryRounds.value = d.memoryRounds ?? 20
    formEnabled.value = d.enabled === true || d.enabled === 1 ? 1 : 0
    const mp = d.modelParams || {}
    for (const def of modelParamDefs) {
      formModelParams[def.key] = (mp as any)[def.key] ?? null
    }
    pendingAvatarFile.value = null
  } catch (err) {
    feedback.value = '加载助手详情失败'
    console.error(err)
    setTimeout(() => close(), 1500)
  }
}

const groupedModelOptions = computed(() =>
    chatStore.modelSelectors.map((s) => ({
      type: 'group' as const,
      label: s.providerName,
      key: s.providerCode,
      children: s.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: m.modelId,
      })),
    }))
)

function onModelSelect(modelId: string | null) {
  if (!modelId) return
  formModelId.value = modelId
  for (const sel of chatStore.modelSelectors) {
    const found = sel.models.find(m => m.modelId === modelId)
    if (found) {
      formProviderId.value = found.providerId
      return
    }
  }
}

function onAvatarPicked(value: string | null, file: File | null) {
  if (file) {
    // 图片上传 → 暂存 file，预览用 blob URL
    pendingAvatarFile.value = file
    formAvatar.value = URL.createObjectURL(file)
  } else if (value) {
    // 文字/emoji
    formAvatar.value = value
    pendingAvatarFile.value = null
  }
  showPicker.value = false
}

async function handleSave() {
  if (!detail.value) return
  if (!formProviderId.value) {
    feedback.value = '⚠️ 请先选择一个模型'
    return
  }

  saving.value = true
  try {
    // 第1步：如果头像有变更，先调头像接口
    if (pendingAvatarFile.value) {
      const fd = new FormData()
      fd.append('file', pendingAvatarFile.value)
      const updated = await updateAssistantAvatar(detail.value.id, fd)
      formAvatar.value = updated.avatar
      pendingAvatarFile.value = null
    } else if (formAvatar.value !== originalAvatar.value) {
      const fd = new FormData()
      fd.append('avatar', formAvatar.value)
      await updateAssistantAvatar(detail.value.id, fd)
    }

    // 第2步：更新其他字段
    const modelParams: Record<string, unknown> = {}
    for (const def of modelParamDefs) {
      const v = formModelParams[def.key]
      if (v !== null) modelParams[def.key] = v
    }

    await assistantStore.update(detail.value.id, {
      name: formName.value,
      avatar: formAvatar.value,
      description: formDescription.value,
      systemPrompt: formSystemPrompt.value,
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

function close() {
  show.value = false
  showPicker.value = false
  emit('update:assistantId', null)
}
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
      <span style="font-weight: 600; font-size: 15px;">⚙️ 助手设置</span>
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
        <label class="label">名称</label>
        <input v-model="formName" class="input" placeholder="助手名称"/>
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
</style>
