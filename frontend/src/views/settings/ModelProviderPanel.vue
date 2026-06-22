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
      />

      <!-- 右侧：服务商详情配置 -->
      <ModelProviderDetail
          v-if="activeProvider"
          :provider="activeProvider"
          :name="editForm.name"
          :base-url="editForm.baseUrl"
          :api-key="editForm.apiKey"
          :fetching="fetching"
          @update:name="editForm.name = $event"
          @update:base-url="editForm.baseUrl = $event"
          @update:api-key="editForm.apiKey = $event"
          @fetch-models="fetchRemoteModels"
          @remove-model="removeModel"
          @open-settings="openModelSettings"
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Cpu} from '@lucide/vue'
import {batchRemoveModels, listProviders, updateModel, updateProvider} from '@/api/modelProvider'
import type {ModelItem, ModelProvider} from '@/types/modelProvider'
import {normalizeCaps} from './modelCapabilities'


import ModelProviderSidebar from './ModelProviderSidebar.vue'
import ModelProviderDetail from './ModelProviderDetail.vue'
import ModelSettingsModal from './ModelSettingsModal.vue'
import AddProviderModal from './AddProviderModal.vue'

const loading = ref(false)
const fetching = ref(false)
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
  } catch (e) {
    console.error('保存失败', e)
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

<style scoped src="./model-provider.css"></style>
