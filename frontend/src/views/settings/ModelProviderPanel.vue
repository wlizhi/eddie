<template>
  <div class="provider-layout">
    <div class="provider-body">
      <!-- 左侧：服务商列表 -->
      <aside class="provider-sidebar">
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Cpu, RefreshCw, Trash2} from '@lucide/vue'
import {listProviders, updateProvider} from '@/api/modelProvider'
import type {ModelProvider} from '@/types/modelProvider'

const loading = ref(false)
const fetching = ref(false)
const providers = ref<ModelProvider[]>([])
const activeProvider = ref<ModelProvider | null>(null)

const editForm = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
})

const currentModels = computed(() => activeProvider.value?.models ?? [])

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
