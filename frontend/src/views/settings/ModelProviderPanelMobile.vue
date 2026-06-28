<!--
  ModelProviderPanelMobile.vue — 移动端模型服务管理

  设计要点：
  - 服务商卡片列表，点击展开/收起详情
  - 展开后显示基本信息 + 模型列表
  - 使用 NModal 弹窗做新增/编辑
-->
<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {NButton, NInput, NModal, NSwitch} from 'naive-ui'
import {Cpu, Plus, RefreshCw, Settings, Trash2} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import {
  batchRemoveModels,
  createProvider,
  deleteProvider,
  listProviders,
  updateModel,
  updateProvider,
} from '@/api/modelProvider'
import type {ModelItem, ModelProvider} from '@/types/modelProvider'
import {CAPABILITY_LABELS} from '@/types/modelProvider'
import {normalizeCaps} from './modelCapabilities'
import {showToast} from '@/composables/useToast'
import ModelFetchModal from './ModelFetchModal.vue'
import ModelSettingsModal from './ModelSettingsModal.vue'

const {iconSizeSm} = useIconSize()

const loading = ref(false)
const providers = ref<ModelProvider[]>([])
/** 当前展开的服务商 id（null = 全部收起） */
const expandedId = ref<number | null>(null)

// ===== 新增服务商弹窗 =====
const showAddModal = ref(false)
const addForm = reactive({code: '', name: '', baseUrl: '', apiKey: '', enabled: 1 as 0 | 1})
const addErrors = reactive({code: '', name: '', baseUrl: ''})
const submitting = ref(false)

// ===== 编辑服务商 =====
const showEditModal = ref(false)
const editingProvider = ref<ModelProvider | null>(null)
const editForm = reactive({name: '', baseUrl: '', apiKey: ''})

// ===== 模型设置 =====
const showModelSettings = ref(false)
const editingModel = ref<ModelItem | null>(null)

// ===== 获取模型弹窗 =====
const showFetchModal = ref(false)
const fetchProviderId = ref<number>(0)

// ===== 已存在的模型 code =====
const existingModelCodes = computed(() => [])

/** 加载服务商列表 */
async function loadProviders() {
  loading.value = true
  try {
    providers.value = await listProviders()
  } catch (e) {
    console.error('加载服务商列表失败', e)
    showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 展开/收起服务商 */
function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

// ===== 启用/禁用 =====
async function toggleEnabled(p: ModelProvider) {
  const newEnabled = p.enabled === 1 ? 0 : 1
  try {
    await updateProvider({id: p.id, enabled: newEnabled})
    const idx = providers.value.findIndex(x => x.id === p.id)
    if (idx !== -1) {
      providers.value[idx] = {...providers.value[idx], enabled: newEnabled}
    }
  } catch (e) {
    console.error('切换启用状态失败', e)
    showToast('切换失败', 'error')
  }
}

// ===== 新增服务商 =====
function validateAdd(): boolean {
  let valid = true
  addErrors.code = ''
  addErrors.name = ''
  addErrors.baseUrl = ''

  if (!addForm.code.trim()) {
    addErrors.code = '请输入服务商 code'
    valid = false
  } else if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(addForm.code.trim())) {
    addErrors.code = 'code 须以字母开头'
    valid = false
  }
  if (!addForm.name.trim()) {
    addErrors.name = '请输入服务商名称'
    valid = false
  }
  if (!addForm.baseUrl.trim()) {
    addErrors.baseUrl = '请输入 API 地址'
    valid = false
  } else {
    try {
      new URL(addForm.baseUrl.trim())
    } catch {
      addErrors.baseUrl = 'URL 格式不正确'
      valid = false
    }
  }
  return valid
}

async function handleAdd() {
  if (!validateAdd()) return
  submitting.value = true
  try {
    await createProvider({
      code: addForm.code.trim(),
      name: addForm.name.trim(),
      baseUrl: addForm.baseUrl.trim().replace(/\/+$/, ''),
      apiKey: addForm.apiKey.trim() || undefined,
      enabled: addForm.enabled,
    })
    showAddModal.value = false
    addForm.code = ''
    addForm.name = ''
    addForm.baseUrl = ''
    addForm.apiKey = ''
    addForm.enabled = 1
    await loadProviders()
    showToast('服务商已添加')
  } catch (e) {
    console.error('新增失败', e)
    showToast('新增失败', 'error')
  } finally {
    submitting.value = false
  }
}

// ===== 编辑服务商 =====
function openEdit(p: ModelProvider) {
  editingProvider.value = p
  editForm.name = p.name
  editForm.baseUrl = p.baseUrl
  editForm.apiKey = p.apiKey
  showEditModal.value = true
}

async function handleEditSave() {
  if (!editingProvider.value) return
  try {
    await updateProvider({
      id: editingProvider.value.id,
      name: editForm.name,
      baseUrl: editForm.baseUrl,
      apiKey: editForm.apiKey,
    })
    showEditModal.value = false
    await loadProviders()
    showToast('已保存')
  } catch (e) {
    console.error('保存失败', e)
    showToast('保存失败', 'error')
  }
}

// ===== 删除服务商 =====
async function handleDelete(p: ModelProvider) {
  try {
    await deleteProvider(p.id)
    providers.value = providers.value.filter(x => x.id !== p.id)
    if (expandedId.value === p.id) expandedId.value = null
    showToast('服务商已删除')
  } catch (e) {
    console.error('删除失败', e)
    showToast('删除失败', 'error')
  }
}

// ===== 移除模型 =====
async function removeModel(providerId: number, code: string) {
  try {
    await batchRemoveModels(providerId, [code])
    const p = providers.value.find(x => x.id === providerId)
    if (p) {
      p.models = p.models.filter(m => m.code !== code)
    }
    showToast('模型已移除')
  } catch (e) {
    console.error('移除模型失败', e)
    showToast('移除模型失败', 'error')
  }
}

// ===== 模型设置 =====
function openModelSettings(m: ModelItem) {
  editingModel.value = m
  showModelSettings.value = true
}

async function saveModelSettings(payload: {
  code: string
  name: string
  capabilities: string[]
  currency: string
  customCurrency: string
  inputPrice: number
  outputPrice: number
  cacheInputPrice?: number
  cacheWriteInputPrice?: number
}) {
  const provider = providers.value.find(p =>
      p.models.some(m => m.code === payload.code)
  )
  if (!provider) return

  const finalCurrency = payload.currency === 'custom'
      ? (payload.customCurrency || '¥')
      : payload.currency

  try {
    await updateModel(provider.id, {
      code: payload.code,
      name: payload.name || payload.code,
      capabilities: [...payload.capabilities],
      currency: finalCurrency,
      inputPrice: payload.inputPrice,
      outputPrice: payload.outputPrice,
      cacheInputPrice: payload.cacheInputPrice,
      cacheWriteInputPrice: payload.cacheWriteInputPrice,
    })
    const p = providers.value.find(x => x.id === provider.id)
    if (p) {
      const mi = p.models.findIndex(m => m.code === payload.code)
      if (mi !== -1) {
        p.models[mi] = {...p.models[mi], ...payload, currency: finalCurrency}
      }
    }
    showModelSettings.value = false
    showToast('模型设置已保存')
  } catch (e) {
    console.error('保存模型设置失败', e)
    showToast('保存失败', 'error')
  }
}

// ===== 获取远程模型 =====
function openFetch(p: ModelProvider) {
  fetchProviderId.value = p.id
  showFetchModal.value = true
}

async function onModelsChanged() {
  await loadProviders()
}

onMounted(loadProviders)
</script>

<template>
  <div class="provider-list-mobile">
    <div v-if="loading" class="loading-mobile">加载中...</div>

    <template v-else>
      <!-- 服务商卡片列表 -->
      <div
          v-for="p in providers"
          :key="p.id"
          class="provider-card-mobile"
          :class="{ expanded: expandedId === p.id }"
      >
        <div class="provider-card-header" @click="toggleExpand(p.id)">
          <Cpu :size="iconSizeSm" :stroke-width="1.5" style="flex-shrink:0;color:var(--accent-default)"/>
          <div class="provider-card-info">
            <div class="provider-card-name" :class="{ disabled: !p.enabled }">
              {{ p.name || p.code }}
            </div>
            <div class="provider-card-code">{{ p.code }}</div>
          </div>
          <div class="provider-card-toggle" @click.stop>
            <NSwitch
                :value="p.enabled === 1"
                @update:value="toggleEnabled(p)"
                size="small"
            />
          </div>
        </div>

        <!-- 展开详情 -->
        <div v-if="expandedId === p.id" class="provider-detail-mobile">
          <div class="prop-row-mobile">
            <span class="prop-label-mobile">Base URL</span>
            <span class="prop-value-mobile">{{ p.baseUrl }}</span>
          </div>
          <div class="prop-row-mobile">
            <span class="prop-label-mobile">API Key</span>
            <span class="prop-value-mobile">{{ p.apiKey ? '••••••••' : '未设置' }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="provider-actions-mobile">
            <button class="action-btn-mobile action-btn-outline" @click="openEdit(p)">
              <Settings :size="14" :stroke-width="1.8"/>
              编辑
            </button>
            <button class="action-btn-mobile action-btn-outline" @click="openFetch(p)">
              <RefreshCw :size="14" :stroke-width="1.8"/>
              获取模型
            </button>
            <button
                v-if="p.builtIn !== 1"
                class="action-btn-mobile action-btn-danger"
                @click="handleDelete(p)"
            >
              <Trash2 :size="14" :stroke-width="1.8"/>
              删除
            </button>
          </div>

          <!-- 分隔线 -->
          <div class="section-divider-mobile">
            <span class="divider-line"/>
            <span style="font-size:var(--font-size-xs);color:var(--text-tertiary);white-space:nowrap">
                            模型 ({{ p.models?.length || 0 }})
                        </span>
            <span class="divider-line"/>
          </div>

          <!-- 模型列表 -->
          <div class="model-list-mobile">
            <div
                v-for="m in (p.models ?? []).map(m => ({...m, capabilities: normalizeCaps(m.capabilities)}))"
                :key="m.code"
                class="model-item-mobile"
            >
              <span class="model-name-mobile">{{ m.code }}</span>
              <div v-if="m.capabilities?.length" class="model-caps-mobile">
                                <span
                                    v-for="cap in m.capabilities.slice(0, 2)"
                                    :key="cap"
                                    class="model-cap-tag-mobile"
                                    :class="cap"
                                >
                                    {{ CAPABILITY_LABELS[cap] || cap }}
                                </span>
              </div>
              <div class="model-item-actions-mobile">
                <button
                    class="model-icon-btn-mobile settings"
                    title="设置"
                    @click="openModelSettings(m)"
                >
                  <Settings :size="14" :stroke-width="1.8"/>
                </button>
                <button
                    class="model-icon-btn-mobile remove"
                    title="移除"
                    @click="removeModel(p.id, m.code)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                       stroke="currentColor" stroke-width="2.5"
                       stroke-linecap="round" stroke-linejoin="round">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                  </svg>
                </button>
              </div>
            </div>
            <div v-if="!p.models?.length" class="models-empty-mobile">
              暂无模型
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="providers.length === 0 && !loading" class="models-empty-mobile" style="padding:var(--space-12) 0">
        <Cpu :size="40" :stroke-width="1" style="color:var(--icon-muted);margin-bottom:var(--space-3)"/>
        <div>暂无服务商</div>
      </div>

      <!-- 新增服务商按钮 -->
      <button
          class="action-btn-mobile action-btn-primary"
          style="margin-top:var(--space-4)"
          @click="showAddModal = true"
      >
        <Plus :size="16" :stroke-width="2.5"/>
        新增服务商
      </button>
    </template>
  </div>

  <!-- ===== 新增服务商弹窗 ===== -->
  <NModal
      :show="showAddModal"
      preset="card"
      title="新增服务商"
      style="max-width:90vw;width:22rem"
      :mask-closable="false"
      @update:show="(v:boolean) => { if (!v) showAddModal = false }"
  >
    <div style="display:flex;flex-direction:column;gap:var(--space-4)">
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
                <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">
                    Code <span style="color:var(--danger-default)">*</span>
                </span>
        <n-input v-model:value="addForm.code" placeholder="如 openai"/>
        <span v-if="addErrors.code" style="font-size:var(--font-size-xs);color:var(--danger-default)">
                    {{ addErrors.code }}
                </span>
      </div>
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
                <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">
                    名称 <span style="color:var(--danger-default)">*</span>
                </span>
        <n-input v-model:value="addForm.name" placeholder="显示名称"/>
        <span v-if="addErrors.name" style="font-size:var(--font-size-xs);color:var(--danger-default)">
                    {{ addErrors.name }}
                </span>
      </div>
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
                <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">
                    Base URL <span style="color:var(--danger-default)">*</span>
                </span>
        <n-input v-model:value="addForm.baseUrl" placeholder="https://api.openai.com/v1"/>
        <span v-if="addErrors.baseUrl" style="font-size:var(--font-size-xs);color:var(--danger-default)">
                    {{ addErrors.baseUrl }}
                </span>
      </div>
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
        <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">API Key</span>
        <n-input v-model:value="addForm.apiKey" type="password" placeholder="可选"/>
      </div>
      <div style="display:flex;align-items:center;gap:var(--space-3)">
        <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">状态</span>
        <NSwitch :value="addForm.enabled === 1" @update:value="(v:boolean) => addForm.enabled = v ? 1 : 0"/>
      </div>
    </div>
    <template #footer>
      <div style="display:flex;justify-content:flex-end;gap:var(--space-3)">
        <NButton @click="showAddModal = false">取消</NButton>
        <NButton type="primary" :loading="submitting" @click="handleAdd">确定</NButton>
      </div>
    </template>
  </NModal>

  <!-- ===== 编辑服务商弹窗 ===== -->
  <NModal
      :show="showEditModal"
      preset="card"
      title="编辑服务商"
      style="max-width:90vw;width:22rem"
      :mask-closable="false"
      @update:show="(v:boolean) => { if (!v) showEditModal = false }"
  >
    <div style="display:flex;flex-direction:column;gap:var(--space-4)">
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
        <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">名称</span>
        <n-input v-model:value="editForm.name"/>
      </div>
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
        <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">Base URL</span>
        <n-input v-model:value="editForm.baseUrl"/>
      </div>
      <div style="display:flex;flex-direction:column;gap:var(--space-1)">
        <span style="font-size:var(--font-size-small);font-weight:500;color:var(--text-secondary)">API Key</span>
        <n-input v-model:value="editForm.apiKey" type="password"/>
      </div>
    </div>
    <template #footer>
      <div style="display:flex;justify-content:flex-end;gap:var(--space-3)">
        <NButton @click="showEditModal = false">取消</NButton>
        <NButton type="primary" @click="handleEditSave">保存</NButton>
      </div>
    </template>
  </NModal>

  <!-- ===== 模型设置弹窗（复用桌面端组件） ===== -->
  <ModelSettingsModal
      v-if="showModelSettings"
      :model="editingModel"
      :visible="showModelSettings"
      @close="showModelSettings = false"
      @save="saveModelSettings"
  />

  <!-- ===== 获取远程模型弹窗（复用桌面端组件） ===== -->
  <ModelFetchModal
      v-if="showFetchModal"
      :visible="showFetchModal"
      :provider-id="fetchProviderId"
      :existing-codes="existingModelCodes"
      @close="showFetchModal = false"
      @changed="onModelsChanged"
  />
</template>

<style scoped src="./model-provider-mobile.css"></style>
