<template>
  <div class="provider-layout">
    <div class="provider-body">
      <!-- 左侧：服务商列表 -->
      <ModelProviderSidebar
          :providers="providers"
          :active-id="activeProvider?.id"
          :loading="loading"
          @select="selectProvider"
          @add="addProvider"
          @toggle="toggleProviderEnabled"
          @sort="handleSort"
      />

      <!-- 右侧：服务商详情配置 -->
      <ModelProviderDetail
          v-if="activeProvider"
          :provider="activeProvider"
          :name="editForm.name"
          :base-url="editForm.baseUrl"
          :api-key="editForm.apiKey"
          @update:name="editForm.name = $event"
          @update:base-url="editForm.baseUrl = $event"
          @update:api-key="editForm.apiKey = $event"
          @fetch-models="openFetchModal"
          @remove-model="removeModel"
          @open-settings="openModelSettings"
          @delete-provider="handleDeleteProvider"
      />

      <!-- 未选中状态 -->
      <div v-else class="provider-detail empty">
        <Cpu :size="40" :stroke-width="1" class="detail-placeholder-icon"/>
        <span class="detail-placeholder-text">请在左侧选择一个模型服务商</span>
      </div>
    </div>

    <!-- 新增服务商弹窗 -->
    <AddProviderModal
        :visible="showAddModal"
        @close="showAddModal = false"
        @created="onProviderCreated"
    />

    <!-- 模型设置弹窗 -->
    <ModelSettingsModal
        v-if="showModal"
        :model="editingModel"
        :visible="showModal"
        @close="closeModal"
        @save="saveModelSettings"
    />

    <!-- 远程模型拉取弹窗 -->
    <ModelFetchModal
        v-if="activeProvider"
        :visible="showFetchModal"
        :provider-id="activeProvider.id"
        :existing-codes="existingModelCodes"
        @close="showFetchModal = false"
        @changed="onModelsChanged"
    />

    <!-- 删除确认弹窗 -->
    <ConfirmModal
        :visible="showDeleteConfirm"
        title="删除服务商"
        :message="`确认删除服务商「${deletingName}」？删除后不可恢复。`"
        @confirm="doDeleteProvider"
        @cancel="showDeleteConfirm = false"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Cpu} from '@lucide/vue'
import {
  batchRemoveModels,
  deleteProvider,
  listProviders,
  updateModel,
  updateProvider,
  updateSortOrder
} from '@/api/modelProvider'
import type {ModelItem, ModelProvider} from '@/types/modelProvider'
import {normalizeCaps} from './modelCapabilities'


import ModelProviderSidebar from './ModelProviderSidebar.vue'
import ModelProviderDetail from './ModelProviderDetail.vue'
import ModelSettingsModal from './ModelSettingsModal.vue'
import AddProviderModal from './AddProviderModal.vue'
import ModelFetchModal from './ModelFetchModal.vue'
import ConfirmModal from './ConfirmModal.vue'

const loading = ref(false)
const providers = ref<ModelProvider[]>([])
const activeProvider = ref<ModelProvider | null>(null)

const editForm = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
})

/** 模板直接使用的模型列表，capabilities 统一转小写 */
const currentModels = computed(() =>
    (activeProvider.value?.models ?? []).map(m => ({
      ...m,
      capabilities: normalizeCaps(m.capabilities),
    }))
)

// ===== 新增服务商弹窗 =====
const showAddModal = ref(false)

/** 打开新增服务商弹窗 */
function addProvider() {
  showAddModal.value = true
}

/** 新增完成后回调 */
async function onProviderCreated() {
  showAddModal.value = false
  try {
    providers.value = await listProviders()
    if (providers.value.length > 0) {
      selectProvider(providers.value[0])
    }
  } catch (e) {
    console.error('重新加载服务商列表失败', e)
  }
}

// ===== 模型设置弹窗 =====
const showModal = ref(false)
const editingModel = ref<ModelItem | null>(null)

/** 打开模型设置弹窗 */
function openModelSettings(m: ModelItem) {
  editingModel.value = m
  showModal.value = true
}

/** 关闭弹窗 */
function closeModal() {
  showModal.value = false
  editingModel.value = null
}

/** 保存模型设置 */
async function saveModelSettings(payload: {
  code: string
  name: string
  capabilities: string[]
  currency: string
  customCurrency: string
  inputPrice: number
  outputPrice: number
}) {
  if (!activeProvider.value || !editingModel.value) return

  const finalCurrency = payload.currency === 'custom'
      ? (payload.customCurrency || '¥')
      : payload.currency

  try {
    await updateModel(activeProvider.value.id, {
      code: editingModel.value.code,
      name: payload.name || editingModel.value.code,
      capabilities: [...payload.capabilities],
      currency: finalCurrency,
      inputPrice: payload.inputPrice,
      outputPrice: payload.outputPrice,
    })

    // 更新本地数据
    activeProvider.value.models = currentModels.value.map(m => {
      if (m.code === editingModel.value!.code) {
        return {
          ...m,
          name: payload.name || m.code,
          capabilities: [...payload.capabilities],
          currency: finalCurrency,
          inputPrice: payload.inputPrice,
          outputPrice: payload.outputPrice,
        }
      }
      return m
    })
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
    // 同步更新到 providers 列表，使左侧 sidebar 响应式更新
    const idx = providers.value.findIndex(p => p.id === activeProvider.value!.id)
    if (idx !== -1) {
      providers.value[idx] = {
        ...providers.value[idx],
        name: editForm.name,
        baseUrl: editForm.baseUrl,
        apiKey: editForm.apiKey,
      }
    }
    // 同步更新 activeProvider
    Object.assign(activeProvider.value, {
      name: editForm.name,
      baseUrl: editForm.baseUrl,
      apiKey: editForm.apiKey,
    })
  } catch (e) {
    console.error('保存失败', e)
  }
}

/** 启用/禁用切换 */
async function toggleProviderEnabled(p: ModelProvider) {
  const newEnabled = p.enabled === 1 ? 0 : 1
  try {
    await updateProvider({id: p.id, enabled: newEnabled})
    // 同步本地数据
    const idx = providers.value.findIndex(x => x.id === p.id)
    if (idx !== -1) {
      providers.value[idx] = {...providers.value[idx], enabled: newEnabled}
    }
    if (activeProvider.value?.id === p.id) {
      activeProvider.value.enabled = newEnabled
    }
  } catch (e) {
    console.error('切换启用状态失败', e)
  }
}

/** 拖拽排序 */
async function handleSort(orderedIds: number[]) {
  try {
    await updateSortOrder(orderedIds)
    // 重新获取完整列表以同步后端排序（含 enabled/disabled 分组）
    providers.value = await listProviders()
    if (activeProvider.value) {
      const updated = providers.value.find(p => p.id === activeProvider.value!.id)
      if (updated) selectProvider(updated)
    }
  } catch (e) {
    console.error('更新排序失败', e)
  }
}

/** 移除某个模型 */
async function removeModel(code: string) {
  if (!activeProvider.value) return
  try {
    await batchRemoveModels(activeProvider.value.id, [code])
    activeProvider.value.models = currentModels.value.filter(m => m.code !== code)
  } catch (e) {
    console.error('移除模型失败', e)
  }
}

/** 删除确认弹窗状态 */
const showDeleteConfirm = ref(false)
const deletingName = ref('')

/** 打开删除确认弹窗 */
function handleDeleteProvider() {
  if (!activeProvider.value) return
  deletingName.value = activeProvider.value.name || activeProvider.value.code
  showDeleteConfirm.value = true
}

/** 执行删除 */
async function doDeleteProvider() {
  if (!activeProvider.value) return
  showDeleteConfirm.value = false
  try {
    await deleteProvider(activeProvider.value.id)
    providers.value = await listProviders()
    if (providers.value.length > 0) {
      selectProvider(providers.value[0])
    } else {
      activeProvider.value = null
    }
  } catch (e) {
    console.error('删除服务商失败', e)
  }
}

// ===== 远程模型拉取弹窗 =====
const showFetchModal = ref(false)

/** 已存在的模型 code 列表，用于弹窗过滤 */
const existingModelCodes = computed(() =>
    (activeProvider.value?.models ?? []).map(m => m.code)
)

/** 打开远程模型拉取弹窗 */
function openFetchModal() {
  showFetchModal.value = true
}

/** 模型变更后刷新数据（不关闭弹窗） */
async function onModelsChanged() {
  try {
    providers.value = await listProviders()
    if (activeProvider.value) {
      const updated = providers.value.find(p => p.id === activeProvider.value!.id)
      if (updated) selectProvider(updated)
    }
  } catch (e) {
    console.error('刷新服务商列表失败', e)
  }
}

/** 模型添加完成后刷新数据（关闭弹窗） */
async function onModelsAdded() {
  showFetchModal.value = false
  // 刷新当前服务商数据
  try {
    providers.value = await listProviders()
    if (activeProvider.value) {
      const updated = providers.value.find(p => p.id === activeProvider.value!.id)
      if (updated) selectProvider(updated)
    }
  } catch (e) {
    console.error('刷新服务商列表失败', e)
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

<style scoped src="./model-provider.css"></style>
