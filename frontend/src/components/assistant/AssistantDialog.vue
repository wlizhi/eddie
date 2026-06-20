<!--
  AssistantDialog.vue — 助手设置编辑弹窗

  功能：
  - 编辑助手名称、头像、描述、System Prompt
  - 切换模型（复用模型选择器数据）
  - 设置记忆轮数
  - 保存到后端
-->
<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
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
        value: `${s.providerId}:${m.modelId}`, // 组合值：providerId:modelId
      })),
    }))
)

/** 当前选中的模型显示值 */
const currentModelValue = computed(() =>
    formProviderId.value && formModelId.value
        ? `${formProviderId.value}:${formModelId.value}`
        : null
)

function onModelSelect(val: string | null) {
  if (!val) return
  const [pid, mid] = val.split(':')
  formProviderId.value = Number(pid)
  formModelId.value = mid
}

/** 保存 */
async function handleSave() {
  if (!detail.value) return
  saving.value = true
  try {
    await assistantStore.update(detail.value.id, {
      name: formName.value,
      avatar: formAvatar.value || undefined,
      description: formDescription.value || undefined,
      systemPrompt: formSystemPrompt.value || undefined,
      providerId: formProviderId.value ?? undefined,
      modelId: formModelId.value || undefined,
      memoryRounds: formMemoryRounds.value,
    })
    feedback.value = '✅ 保存成功'
    setTimeout(() => close(), 800)
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

/** 首次打开时加载模型列表 */
onMounted(() => {
  if (chatStore.modelSelectors.length === 0) {
    chatStore.loadModels()
  }
})
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
            :value="currentModelValue"
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
</style>
