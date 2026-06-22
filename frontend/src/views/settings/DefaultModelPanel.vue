<template>
  <div class="panel">
    <div v-for="slot in modelSlots" :key="slot.key" class="config-card">
      <div class="config-header">
        <component :is="slot.icon" :size="18" :stroke-width="2" :class="slot.iconClass"/>
        <span>{{ slot.label }}</span>
      </div>
      <p class="config-hint">{{ slot.hint }}</p>

      <!-- 模型选择 -->
      <div class="config-row">
        <span class="config-label">模型</span>
        <div class="nselect-wrap">
          <NSelect
              :value="slotState(slot.key).compositeKey"
              :options="groupedModelOptions"
              placeholder="选择模型"
              :consistent-menu-width="false"
              @update:value="(v: string | null) => onSlotModelSelect(slot.key, v)"
          />
        </div>
      </div>

      <!-- 模型参数 -->
      <div class="config-row params-row">
        <span class="config-label">参数</span>
        <div class="params-grid">
          <div class="param-item" v-for="def in MODEL_PARAM_DEFS" :key="def.key">
            <span class="param-label">{{ def.label }}</span>
            <input
                v-model.number="slotParams[slot.key][def.key]"
                type="number"
                :step="def.step"
                :min="def.min"
                :max="def.max"
                class="param-input"
                placeholder="默认"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 反馈 -->
    <div v-if="feedback" class="feedback" :class="{ error: feedbackType === 'error' }">
      {{ feedback }}
    </div>

    <!-- 保存按钮 -->
    <div class="save-bar">
      <button class="btn-save" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中...' : '保存配置' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {NSelect} from 'naive-ui'
import {Globe, Zap} from '@lucide/vue'
import {useChatStore} from '@/stores/chat'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'

// ========== 三个模型槽位定义 ==========
interface ModelSlot {
  key: 'DEFAULT_MODEL' | 'FAST_MODEL' | 'TRANSLATE_MODEL'
  label: string
  hint: string
  icon: any
  iconClass: string
}

const modelSlots: ModelSlot[] = [
  {
    key: 'DEFAULT_MODEL',
    label: '默认模型',
    hint: '创建助手时，未指定模型则使用此模型。建议选择综合能力强的模型。',
    icon: Zap,
    iconClass: '',
  },
  {
    key: 'FAST_MODEL',
    label: '快速模型',
    hint: '用于生成会话标题、中期记忆压缩、长期记忆摘要等轻量杂活。建议选择便宜快速的模型。',
    icon: Zap,
    iconClass: 'fast-icon',
  },
  {
    key: 'TRANSLATE_MODEL',
    label: '翻译模型',
    hint: '翻译功能专用模型。可根据需要选择翻译能力强的模型。',
    icon: Globe,
    iconClass: 'translate-icon',
  },
]

// ========== 模型选择器数据（复用 chatStore） ==========
const chatStore = useChatStore()
const MODEL_KEY_SEPARATOR = '::'

const groupedModelOptions = computed(() =>
    chatStore.modelSelectors.map((s) => ({
      type: 'group' as const,
      label: s.providerName,
      key: s.providerCode,
      children: s.models.map((m) => ({
        label: m.displayName ?? m.modelId,
        value: `${m.providerId}${MODEL_KEY_SEPARATOR}${m.modelId}`,
      })),
    }))
)

// ========== 每个槽位的选中状态 ==========
interface SlotSelection {
  providerId: number | null
  modelId: string
  compositeKey: string | null
}

const slotSelections = reactive<Record<string, SlotSelection>>({})

function slotState(key: string): SlotSelection {
  if (!slotSelections[key]) {
    slotSelections[key] = {providerId: null, modelId: '', compositeKey: null}
  }
  return slotSelections[key]
}

function onSlotModelSelect(slotKey: string, compositeKey: string | null) {
  if (!compositeKey) return
  const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
  if (sepIdx === -1) return
  const providerId = Number(compositeKey.substring(0, sepIdx))
  const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
  if (!providerId || !modelId) return
  const s = slotSelections[slotKey]
  if (s) {
    s.providerId = providerId
    s.modelId = modelId
    s.compositeKey = compositeKey
  }
}

// ========== 每个槽位的模型参数 ==========
const slotParams = reactive<Record<string, Record<string, number | null>>>({})
for (const slot of modelSlots) {
  slotParams[slot.key] = Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, null]))
}

// ========== 状态 ==========
const saving = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error'>('success')

// ========== 初始化加载 ==========
onMounted(async () => {
  // 加载模型列表
  if (chatStore.modelSelectors.length === 0) {
    await chatStore.loadModels()
  }

  // 加载全局配置
  try {
    const configs = await fetchConfigs()
    for (const slot of modelSlots) {
      const raw = configs[slot.key]
      if (!raw || raw === '{}') continue
      try {
        const val = JSON.parse(raw)
        const providerId = val.providerId
        const modelId = val.modelId
        const params = val.modelParams || {}

        if (providerId && modelId) {
          slotSelections[slot.key] = {
            providerId,
            modelId,
            compositeKey: `${providerId}${MODEL_KEY_SEPARATOR}${modelId}`,
          }
        }

        // 回填参数
        const p = slotParams[slot.key]
        for (const def of MODEL_PARAM_DEFS) {
          const v = params[def.key]
          if (v !== undefined && v !== null) {
            p[def.key] = v
          }
        }
      } catch {
        // JSON 解析失败则忽略
      }
    }
  } catch (err: any) {
    feedback.value = '加载配置失败: ' + (err.message || '未知错误')
    feedbackType.value = 'error'
  }
})

// ========== 保存 ==========
async function handleSave() {
  feedback.value = ''
  saving.value = true

  try {
    const payload: Record<string, string> = {}

    for (const slot of modelSlots) {
      const sel = slotSelections[slot.key]
      if (!sel?.providerId || !sel?.modelId) {
        // 未配置的槽位保持原样（不提交空值，PUT 时会删除旧数据）
        // 用空对象表示未配置
        payload[slot.key] = '{}'
        continue
      }

      const params: Record<string, number> = {}
      for (const def of MODEL_PARAM_DEFS) {
        const v = slotParams[slot.key][def.key]
        if (v !== null) {
          params[def.key] = v
        }
      }

      payload[slot.key] = JSON.stringify({
        providerId: sel.providerId,
        modelId: sel.modelId,
        modelParams: Object.keys(params).length > 0 ? params : {},
      })
    }

    await updateConfigs(payload)
    feedback.value = '✅ 配置已保存'
    feedbackType.value = 'success'
  } catch (err: any) {
    feedback.value = '❌ 保存失败: ' + (err.message || '未知错误')
    feedbackType.value = 'error'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.panel {
  max-width: 640px;
}

.config-card {
  border: 1px solid #e6e8ec;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: #fafbfc;
}

.config-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
}

.config-header .fast-icon {
  color: #10b981;
}

.config-header .translate-icon {
  color: #8b5cf6;
}

.config-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 14px;
  line-height: 1.5;
}

.config-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 6px 0;
}

.config-label {
  width: 56px;
  font-size: 13px;
  color: #6b7280;
  flex-shrink: 0;
  padding-top: 4px;
}

.nselect-wrap {
  flex: 1;
  min-width: 0;
}

/* 参数区域 */
.params-row {
  flex-direction: column;
  gap: 8px;
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  width: 100%;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
}

.param-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #d9dce0;
  border-radius: 6px;
  font-size: 12px;
  color: #1f1f1f;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.param-input:focus {
  border-color: #409eff;
}

.param-input::placeholder {
  color: #c0c4cc;
}

/* 反馈 */
.feedback {
  font-size: 13px;
  padding: 8px 0;
  margin-bottom: 12px;
}

.feedback.error {
  color: #e74c3c;
}

/* 保存栏 */
.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.btn-save {
  padding: 8px 24px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-save:hover {
  background: #337ecc;
}

.btn-save:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
</style>
