<template>
  <div class="panel">
    <!-- ===== 搜索设置 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Search :size="16" :stroke-width="2" class="group-icon"/>
        搜索设置
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">搜索结果数量</span>
          <span class="setting-hint">内置搜索工具每次返回的结果数量（1~20）</span>
        </div>
        <n-input-number
            v-model:value="searchResultCount"
            :min="1"
            :max="20"
            :step="1"
            :show-button="false"
            class="number-input"
            placeholder="8"
        />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">网页抓取最大字符数</span>
          <span class="setting-hint">抓取网页内容时的最大字符数限制（1,000~15,000）</span>
        </div>
        <n-input-number
            v-model:value="webFetchMaxChars"
            :min="1000"
            :max="15000"
            :step="500"
            :show-button="false"
            class="number-input"
            placeholder="4000"
        />
      </div>
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
import {onMounted, ref} from 'vue'
import {NInputNumber} from 'naive-ui'
import {Search} from '@lucide/vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {showToast} from '@/composables/useToast'

const SEARCH_RESULT_COUNT_KEY = 'SEARCH_RESULT_COUNT'
const WEB_FETCH_MAX_CHARS_KEY = 'WEB_FETCH_MAX_CHARS'

const searchResultCount = ref<number | null>(null)
const webFetchMaxChars = ref<number | null>(null)
const saving = ref(false)

onMounted(async () => {
  try {
    const configs = await fetchConfigs()

    const srcRaw = configs[SEARCH_RESULT_COUNT_KEY]
    if (srcRaw && srcRaw !== '{}') {
      const n = parseInt(srcRaw, 10)
      if (!isNaN(n)) searchResultCount.value = Math.min(Math.max(n, 1), 20)
    }

    const wfmcRaw = configs[WEB_FETCH_MAX_CHARS_KEY]
    if (wfmcRaw && wfmcRaw !== '{}') {
      const n = parseInt(wfmcRaw, 10)
      if (!isNaN(n)) webFetchMaxChars.value = Math.min(Math.max(n, 1000), 15000)
    }
  } catch (err: any) {
    showToast('加载配置失败: ' + (err.message || '未知错误'), 'error')
  }
})

async function handleSave() {
  saving.value = true

  try {
    const payload: Record<string, string> = {}
    payload[SEARCH_RESULT_COUNT_KEY] = searchResultCount.value != null ? String(searchResultCount.value) : ''
    payload[WEB_FETCH_MAX_CHARS_KEY] = webFetchMaxChars.value != null ? String(webFetchMaxChars.value) : ''
    await updateConfigs(payload)
    showToast('配置已保存')
  } catch (err: any) {
    showToast('保存失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.panel {
  max-width: 30rem;
}

.settings-group {
  margin-bottom: 24px;
}

.group-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-lighter);
}

.group-icon {
  color: var(--text-tertiary);
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.setting-info {
  flex: 1;
  min-width: 0;
}

.setting-label {
  display: block;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.setting-hint {
  display: block;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.4;
}

.number-input {
  width: 120px;
}

/* 保存栏 */
.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.btn-save {
  padding: 8px 24px;
  background: var(--accent-default);
  color: var(--text-inverse);
  border: none;
  border-radius: 8px;
  font-size: var(--font-size-base);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}

.btn-save:hover {
  background: var(--accent-hover);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
