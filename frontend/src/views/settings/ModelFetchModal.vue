<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="fetch-modal">
      <div class="modal-header">
        <h3>获取模型列表</h3>
        <button class="modal-close" @click="$emit('close')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <div class="modal-body">
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
          <!-- 统计信息 -->
          <div class="fetch-stats">
            <span>共 {{ models.length }} 个模型</span>
            <span v-if="existingCount > 0" class="fetch-existing-count">
              已添加 {{ existingCount }} 个
            </span>
          </div>

          <!-- 列表 -->
          <div class="fetch-list" ref="listRef">
            <div
                v-for="m in models"
                :key="m.code"
                class="fetch-row"
                :class="{ exists: isExist(m.code) }"
            >
              <div class="fetch-cell code">{{ m.code }}</div>
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

      <div class="modal-footer">
        <button class="btn-cancel" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import type {ModelItem} from '@/types/modelProvider'
import {batchAddModels, batchRemoveModels, fetchRemoteModels} from '@/api/modelProvider'

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

/** 打开弹窗时自动拉取 */
watch(() => props.visible, async (v) => {
  if (!v) return
  error.value = ''
  loading.value = true
  models.value = []
  existingSet.value = new Set(props.existingCodes)
  try {
    models.value = await fetchRemoteModels(props.providerId)
  } catch (e: any) {
    error.value = e.message || '拉取失败'
  } finally {
    loading.value = false
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
    error.value = e.message || (isExist(code) ? '移除失败' : '添加失败')
  } finally {
    const s = new Set(busyCodes.value)
    s.delete(code)
    busyCodes.value = s
  }
}
</script>

<style scoped>
/* ===== overlay + modal 容器 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.fetch-modal {
  background: #ffffff;
  border-radius: 12px;
  width: 720px;
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

/* ===== header ===== */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e6e8ec;
  flex-shrink: 0;
}

.modal-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 0;
}

.modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.modal-close:hover {
  background: #f0f1f3;
  color: #1f1f1f;
}

/* ===== body ===== */
.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 加载状态 */
.fetch-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 24px;
  font-size: 14px;
  color: #6b7280;
}

.fetch-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e6e8ec;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 错误提示 */
.fetch-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 24px;
  font-size: 13px;
  color: #ef4444;
}

/* 统计 */
.fetch-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.fetch-existing-count {
  color: #10b981;
  font-weight: 500;
}

/* 列表区域 */
.fetch-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  border: 1px solid #e6e8ec;
  border-radius: 8px;
}

.fetch-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f1f3;
  gap: 12px;
}

.fetch-row:last-child {
  border-bottom: none;
}

.fetch-row.exists {
  background: #f6fef9;
}

/* 单元格 */
.fetch-cell {
  font-size: 13px;
  color: #1f1f1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fetch-cell.code {
  font-family: Monaco, 'Fira Code', monospace;
  flex: 1;
  min-width: 0;
}

.fetch-cell.owned-by {
  width: 100px;
  flex-shrink: 0;
  text-align: right;
  color: #9ca3af;
  font-size: 12px;
}

.fetch-empty {
  padding: 32px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
}

/* 操作按钮 */
.fetch-action-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.fetch-action-btn.btn-add {
  background: #eef2ff;
  color: #2563eb;
}

.fetch-action-btn.btn-add:hover:not(:disabled) {
  background: #2563eb;
  color: #ffffff;
}

.fetch-action-btn.btn-remove {
  background: #fef2f2;
  color: #ef4444;
}

.fetch-action-btn.btn-remove:hover:not(:disabled) {
  background: #ef4444;
  color: #ffffff;
}

.fetch-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-spin {
  animation: spin 0.7s linear infinite;
}

/* ===== footer ===== */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #e6e8ec;
  flex-shrink: 0;
}

.btn-cancel {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #e6e8ec;
  border-radius: 6px;
  background: #ffffff;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.btn-cancel:hover {
  border-color: #d1d5db;
  color: #1f1f1f;
}
</style>
