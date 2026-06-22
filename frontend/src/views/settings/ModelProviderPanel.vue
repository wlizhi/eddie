<template>
  <div class="provider-layout">
    <div class="provider-body">
      <!-- 左侧：服务商列表 -->
      <aside class="provider-sidebar">
        <div class="sidebar-list">
          <div
              v-for="p in providers"
              :key="p.id"
              class="provider-item"
              :class="{ active: activeProvider?.id === p.id }"
              @click="selectProvider(p)"
          >
            <div class="provider-name">{{ p.name || p.code }}</div>
            <div class="provider-code">{{ p.code }}</div>
          </div>
          <div v-if="providers.length === 0 && !loading" class="sidebar-empty">
            暂无服务商
          </div>
          <div v-if="loading" class="sidebar-loading">加载中...</div>
        </div>
        <button class="sidebar-add-btn" @click="addProvider">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新增服务商
        </button>
      </aside>

      <!-- 右侧：服务商详情配置 -->
      <div v-if="activeProvider" class="provider-detail">
        <!-- 属性设置 -->
        <div class="props-section">
          <div class="prop-row">
            <label class="prop-label">Code</label>
            <input
                :value="activeProvider.code"
                class="prop-input"
                disabled
            />
          </div>
          <div class="prop-row">
            <label class="prop-label">名称</label>
            <input
                v-model="editForm.name"
                class="prop-input"
                :disabled="activeProvider.builtIn === 1"
                @change="saveProvider"
            />
          </div>
          <div class="prop-row">
            <label class="prop-label">Base URL</label>
            <input
                v-model="editForm.baseUrl"
                class="prop-input"
                @change="saveProvider"
            />
          </div>
          <div class="prop-row">
            <label class="prop-label">API Key</label>
            <input
                v-model="editForm.apiKey"
                type="password"
                class="prop-input"
                @change="saveProvider"
            />
          </div>
        </div>

        <!-- 分隔线 + 操作按钮 -->
        <div class="section-divider">
          <span class="divider-line"/>
          <button
              class="fetch-btn"
              :disabled="fetching"
              @click="fetchRemoteModels"
          >
            <RefreshCw :size="14" :stroke-width="1.8" :class="{ spinning: fetching }"/>
            获取模型列表
          </button>
        </div>

        <!-- 已添加模型列表 -->
        <div class="models-section">
          <div
              v-for="m in currentModels"
              :key="m.code"
              class="model-row"
          >
            <span class="model-name">{{ m.code }}</span>
            <!-- 能力标签 -->
            <div class="model-capabilities" v-if="m.capabilities?.length">
              <span
                  v-for="cap in m.capabilities"
                  :key="cap"
                  class="cap-tag"
                  :class="cap"
              >
                <span v-html="capIcon(cap, 11)"></span>
                {{ CAPABILITY_LABELS[cap] || cap }}
              </span>
            </div>
            <button
                class="btn-settings"
                title="模型设置"
                @click="openModelSettings(m)"
            >
              <Settings :size="14" :stroke-width="1.8"/>
            </button>
            <button
                class="btn-remove"
                title="移除模型"
                @click="removeModel(m.code)"
            >
              <Trash2 :size="14" :stroke-width="1.8"/>
            </button>
          </div>
          <div v-if="currentModels.length === 0" class="models-empty">
            暂无已添加的模型，点击上方"获取模型列表"拉取
          </div>
        </div>
      </div>

      <!-- 未选中状态 -->
      <div v-else class="provider-detail empty">
        <Cpu :size="40" :stroke-width="1" class="detail-placeholder-icon"/>
        <span class="detail-placeholder-text">请在左侧选择一个模型服务商</span>
      </div>
    </div>

    <!-- 模型设置弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>模型设置</h3>
          <button class="modal-close" @click="closeModal">
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
            <input class="field-input" :value="modalForm.code" disabled/>
          </div>

          <!-- 模型名称（可自定义） -->
          <div class="modal-field">
            <span class="field-label">模型名称</span>
            <input class="field-input" v-model="modalForm.name" placeholder="自定义名称，留空则使用模型 ID"/>
          </div>

          <!-- 能力选择 -->
          <div class="modal-field">
            <span class="field-label">支持能力</span>
            <div class="capability-grid">
              <div
                  v-for="cap in CAPABILITY_TYPES"
                  :key="cap.code"
                  class="cap-option"
                  :class="{ selected: modalForm.capabilities.includes(cap.code) }"
                  :style="getCapStyle(cap.code, modalForm.capabilities.includes(cap.code))"
                  @click="toggleCapability(cap.code)"
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
                  :class="{ active: modalForm.currency === '¥' }"
                  @click="modalForm.currency = '¥'"
              >¥
              </button>
              <button
                  class="currency-option"
                  :class="{ active: modalForm.currency === '$' }"
                  @click="modalForm.currency = '$'"
              >$
              </button>
              <button
                  class="currency-option"
                  :class="{ active: modalForm.currency === 'custom' }"
                  @click="modalForm.currency = 'custom'"
              >自定义
              </button>
              <input
                  v-if="modalForm.currency === 'custom'"
                  v-model="modalForm.customCurrency"
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
                  v-model.number="modalForm.inputPrice"
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
                  v-model.number="modalForm.outputPrice"
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
          <button class="btn-cancel" @click="closeModal">取消</button>
          <button class="btn-save" @click="saveModelSettings">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Cpu, RefreshCw, Settings, Trash2} from '@lucide/vue'
import {listProviders, updateProvider} from '@/api/modelProvider'
import type {ModelItem, ModelProvider} from '@/types/modelProvider'
import {CAPABILITY_LABELS} from '@/types/modelProvider'

const loading = ref(false)
const fetching = ref(false)
const providers = ref<ModelProvider[]>([])
const activeProvider = ref<ModelProvider | null>(null)

const editForm = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
})

/** 后端返回的能力名是大写(VISION)，转小写统一处理 */
function normalizeCaps(caps?: string[]): string[] {
  return (caps ?? []).map(c => c.toLowerCase())
}

/** 模板直接使用的模型列表，capabilities 统一转小写 */
const currentModels = computed(() =>
    (activeProvider.value?.models ?? []).map(m => ({
      ...m,
      capabilities: (m.capabilities ?? []).map(c => c.toLowerCase()),
    }))
)

/** 各能力对应的 SVG 图标 */
function capIcon(code: string, size = 12): string {
  const svgs: Record<string, string> = {
    vision: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>`,
    web_search: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>`,
    reasoning: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4c0 1.5-.8 2.8-2 3.5V12h-4V9.5A4 4 0 0 1 8 6a4 4 0 0 1 4-4z"/><path d="M9 15h6v2H9z"/><path d="M10 19h4v3h-4z"/></svg>`,
    function_calling: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>`,
    rerank: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 3-3 3 3"/><path d="M6 6v12"/><path d="m15 15 3 3 3-3"/><path d="M18 18V6"/></svg>`,
    embedding: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>`,
  }
  return svgs[code] || ''
}

/** 内建能力的 code 与中文名映射 */
const CAPABILITY_TYPES = [
  {code: 'vision', label: '视觉'},
  {code: 'web_search', label: '联网'},
  {code: 'reasoning', label: '推理'},
  {code: 'function_calling', label: '工具'},
  {code: 'rerank', label: '重排'},
  {code: 'embedding', label: '嵌入'},
]

/** 互斥能力组（重排和嵌入单选项） */
const EXCLUSIVE_CAPS = ['rerank', 'embedding']

/** 多选能力组（可多选，与互斥组互斥） */
const MULTI_CAPS = ['vision', 'web_search', 'reasoning', 'function_calling']

/** 各能力的颜色 */
const CAP_COLORS: Record<string, string> = {
  vision: '#7c3aed',
  web_search: '#2563eb',
  reasoning: '#d97706',
  function_calling: '#059669',
  rerank: '#db2777',
  embedding: '#4f46e5',
}

// ===== 弹窗状态 =====
const showModal = ref(false)
const editingModel = ref<ModelItem | null>(null)
const modalForm = reactive({
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
  if (modalForm.currency === 'custom') return modalForm.customCurrency || '¥'
  return modalForm.currency
})

/** 获取能力选项的样式：选中=彩色，未选中=灰色 */
function getCapStyle(code: string, selected: boolean) {
  if (!selected) return {}
  return {color: CAP_COLORS[code] || '#6b7280', borderColor: CAP_COLORS[code] || '#6b7280'}
}

/** 切换能力选中状态（全部可选，互斥组自动取消选中） */
function toggleCapability(code: string) {
  const idx = modalForm.capabilities.indexOf(code)

  if (idx >= 0) {
    // 已选中 → 取消选中
    modalForm.capabilities.splice(idx, 1)
  } else {
    if (EXCLUSIVE_CAPS.includes(code)) {
      // 点击互斥组（重排/嵌入）：清空全部，只选它
      modalForm.capabilities.length = 0
    } else {
      // 点击多选组（视觉/联网/推理/工具）：清除互斥组的选中
      modalForm.capabilities = modalForm.capabilities.filter(c => !EXCLUSIVE_CAPS.includes(c))
    }
    modalForm.capabilities.push(code)
  }
}

/** 打开模型设置弹窗 */
function openModelSettings(m: ModelItem) {
  editingModel.value = m
  modalForm.code = m.code
  modalForm.name = m.code
  modalForm.capabilities = normalizeCaps(m.capabilities)
  modalForm.currency = m.currency || '¥'
  modalForm.customCurrency = m.currency && !['¥', '$'].includes(m.currency) ? m.currency : '¥'
  if (m.currency && !['¥', '$'].includes(m.currency)) {
    modalForm.currency = 'custom'
  }
  modalForm.inputPrice = m.inputPrice ?? 0
  modalForm.outputPrice = m.outputPrice ?? 0
  showModal.value = true
}

/** 关闭弹窗 */
function closeModal() {
  showModal.value = false
  editingModel.value = null
}

/** 保存模型设置 */
async function saveModelSettings() {
  if (!activeProvider.value || !editingModel.value) return

  const finalCurrency = modalForm.currency === 'custom'
      ? (modalForm.customCurrency || '¥')
      : modalForm.currency

  const updated = currentModels.value.map(m => {
    if (m.code === editingModel.value!.code) {
      return {
        ...m,
        name: modalForm.name || m.code,
        capabilities: [...modalForm.capabilities],
        currency: finalCurrency,
        inputPrice: modalForm.inputPrice,
        outputPrice: modalForm.outputPrice,
      }
    }
    return m
  })

  try {
    await updateProvider({
      id: activeProvider.value.id,
      models: JSON.stringify(updated),
    })
    activeProvider.value.models = updated
    closeModal()
  } catch (e) {
    console.error('保存模型设置失败', e)
  }
}

/** 选中某个服务商 */
function selectProvider(p: ModelProvider) {
  activeProvider.value = p
  editForm.name = p.name
  editForm.baseUrl = p.baseUrl
  editForm.apiKey = p.apiKey
}

/** 新增服务商（占位） */
async function addProvider() {
  // TODO: 调用后端 POST /api/model-provider 创建空服务商
  // 接口待实现
  console.warn('add-provider 接口尚未实现')
}

/** 监听 editForm 变化，自动保存 */
let saveTimer: ReturnType<typeof setTimeout> | null = null
watch([() => editForm.name, () => editForm.baseUrl, () => editForm.apiKey], () => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => saveProvider(), 800)
})

/** 保存当前服务商修改 */
async function saveProvider() {
  if (!activeProvider.value) return
  try {
    await updateProvider({
      id: activeProvider.value.id,
      name: editForm.name,
      baseUrl: editForm.baseUrl,
      apiKey: editForm.apiKey,
    })
  } catch (e) {
    console.error('保存失败', e)
  }
}

/** 移除某个模型 */
async function removeModel(code: string) {
  if (!activeProvider.value) return
  const updated = currentModels.value.filter(m => m.code !== code)
  const modelsJson = JSON.stringify(updated)
  try {
    await updateProvider({
      id: activeProvider.value.id,
      models: modelsJson,
    })
    activeProvider.value.models = updated
  } catch (e) {
    console.error('移除模型失败', e)
  }
}

/** 拉取远程模型列表（占位，待后端接口实现后完善） */
async function fetchRemoteModels() {
  if (!activeProvider.value) return
  fetching.value = true
  try {
    // TODO: 调用后端 POST /api/model-provider/{code}/fetch-models 接口
    // 接口待实现
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.warn('fetch-models 接口尚未实现')
  } catch (e) {
    console.error('拉取失败', e)
  } finally {
    fetching.value = false
  }
}

/** 初始化加载 */
onMounted(async () => {
  loading.value = true
  try {
    providers.value = await listProviders()
    if (providers.value.length > 0) {
      selectProvider(providers.value[0])
    }
  } catch (e) {
    console.error('加载服务商列表失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style src="./model-provider.css" scoped/>
