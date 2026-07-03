<!--
 * @author Eddie
 * @date 2026-06-22
-->

<template>
  <NModal
      :show="visible"
      preset="card"
      title="模型列表"
      style="max-width: 50em; width: 90%; max-height: 90vh;"
      content-style="padding: 0; display: flex; flex-direction: column; overflow: hidden;"
      :mask-closable="false"
      @update:show="(v: boolean) => { if (!v) $emit('close') }"
  >
    <div class="fetch-modal-body">
      <!-- 加载中 -->
      <div v-if="loading" class="fetch-loading">
        <span class="fetch-spinner"/>
        正在拉取远程模型列表...
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="fetch-error">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ error }}
      </div>

      <!-- 模型列表 -->
      <template v-else>
        <!-- 搜索栏 + 统计信息（同一行） -->
        <div class="fetch-toolbar">
          <div class="fetch-search">
            <n-input
                v-model:value="searchQuery"
                placeholder="搜索模型 code..."
                clearable
            >
              <template #prefix>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </template>
            </n-input>
          </div>
          <div class="fetch-stats">
            <span v-if="searchQuery">
              搜索到 {{ filteredModels.length }} / 共 {{ models.length }} 个模型
            </span>
            <span v-else>共 {{ models.length }} 个模型</span>
            <span v-if="existingCount > 0" class="fetch-existing-count">
              已添加 {{ existingCount }} 个
            </span>
          </div>
        </div>

        <!-- 列表 -->
        <div class="fetch-list" ref="listRef">
          <div
              v-for="m in filteredModels"
              :key="m.code"
              class="fetch-row"
              :class="{ exists: isExist(m.code) }"
          >
            <div class="fetch-cell code">{{ m.code }}</div>
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
            <div class="fetch-cell owned-by">{{ m.ownedBy || '-' }}</div>
            <button
                class="fetch-action-btn"
                :class="isExist(m.code) ? 'btn-remove' : 'btn-add'"
                :disabled="busyCodes.has(m.code)"
                @click="toggleModel(m)"
                :title="isExist(m.code) ? '移除模型' : '添加模型'"
            >
              <svg v-if="busyCodes.has(m.code)" class="btn-spin" width="14" height="14" viewBox="0 0 24 24"
                   fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/>
              </svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line v-if="isExist(m.code)" x1="5" y1="12" x2="19" y2="12"/>
                <template v-else>
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </template>
              </svg>
            </button>
          </div>
          <div v-if="models.length === 0" class="fetch-empty">
            远程未返回任何模型
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="fetch-modal-footer">
        <button class="btn-cancel" @click="$emit('close')">关闭</button>
      </div>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import {NInput, NModal} from 'naive-ui'
import {computed, onMounted, ref, watch} from 'vue'
import type {ModelItem} from '@/types/modelProvider'
import {CAPABILITY_LABELS} from '@/types/modelProvider'
import {batchAddModels, batchRemoveModels, fetchRemoteModels} from '@/api/modelProvider'
import {showToast} from '@/composables/useToast'
import {capIcon, normalizeCaps} from './modelCapabilities'

const searchQuery = ref('')

/** 模型列表，capabilities 统一转小写 */
const normalizedModels = computed(() =>
    models.value.map(m => ({
      ...m,
      capabilities: normalizeCaps(m.capabilities),
    }))
)

const filteredModels = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return normalizedModels.value
  return normalizedModels.value.filter(m => m.code.toLowerCase().includes(q))
})

const props = defineProps<{
  visible: boolean
  providerId: number
  /** 已存在的模型 code 列表 */
  existingCodes: string[]
}>()

const emit = defineEmits<{
  close: []
  added: []
}>()

const loading = ref(false)
const error = ref('')
const models = ref<ModelItem[]>([])
const listRef = ref<HTMLElement | null>(null)

/** 本地维护的已存在 code 集合（随操作实时更新） */
const existingSet = ref(new Set<string>())
/** 正在操作中的 code 集合（控制按钮 loading） */
const busyCodes = ref(new Set<string>())

const existingCount = computed(() => existingSet.value.size)

function isExist(code: string): boolean {
  return existingSet.value.has(code)
}

/** 拉取远程模型列表 */
async function loadModels() {
  error.value = ''
  loading.value = true
  models.value = []
  existingSet.value = new Set(props.existingCodes)
  try {
    models.value = await fetchRemoteModels(props.providerId)
  } catch (e: any) {
    const msg = e.message || '拉取远程模型列表失败'
    error.value = msg
    showToast(msg, 'error')
  } finally {
    loading.value = false
  }
}

/** 首次挂载时拉取（兼容 v-if 创建组件时 visible 已为 true 的场景） */
onMounted(() => {
  if (props.visible) {
    loadModels()
  }
})

/** visible 变化时拉取 */
watch(() => props.visible, (v) => {
  if (v) {
    loadModels()
  }
})

async function toggleModel(m: ModelItem) {
  const code = m.code
  if (busyCodes.value.has(code)) return

  busyCodes.value = new Set([...busyCodes.value, code])
  try {
    if (isExist(code)) {
      await batchRemoveModels(props.providerId, [code])
      const s = new Set(existingSet.value)
      s.delete(code)
      existingSet.value = s
    } else {
      await batchAddModels(props.providerId, [{
        code: m.code,
        name: m.code,
        object: m.object,
        ownedBy: m.ownedBy,
        capabilities: m.capabilities,
        currency: m.currency,
        inputPrice: m.inputPrice,
        outputPrice: m.outputPrice,
      }])
      const s = new Set(existingSet.value)
      s.add(code)
      existingSet.value = s
    }
    emit('changed')
  } catch (e: any) {
    const msg = e.message || (isExist(code) ? '移除失败' : '添加失败')
    error.value = msg
    showToast(msg, 'error')
  } finally {
    const s = new Set(busyCodes.value)
    s.delete(code)
    busyCodes.value = s
  }
}
</script>

<style src="./model-fetch-modal.css" scoped/>
