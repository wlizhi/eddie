<!--
 * @author Eddie
 * @date 2026-06-22
-->

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
            @blur="saveField(SEARCH_RESULT_COUNT_KEY, searchResultCount)"
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
            @blur="saveField(WEB_FETCH_MAX_CHARS_KEY, webFetchMaxChars)"
        />
      </div>
    </div>

    <!-- ===== 会话标题设置 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Sparkles :size="16" :stroke-width="2" class="group-icon"/>
        会话标题设置
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">自动生成标题</span>
          <span class="setting-hint">首轮对话完成后自动调用 AI 生成会话标题</span>
        </div>
        <n-switch
            :value="enableAutoTitle"
            @update:value="onAutoTitleChange"
        />
      </div>

      <div v-if="enableAutoTitle" class="setting-row">
        <div class="setting-info">
          <span class="setting-label">取前几轮对话</span>
          <span class="setting-hint">基于前 N 轮用户与助手的对话内容生成标题（1~5）</span>
        </div>
        <n-input-number
            v-model:value="titleGenerationRounds"
            :min="1"
            :max="5"
            :step="1"
            :show-button="false"
            class="number-input"
            placeholder="1"
            @blur="saveField(TITLE_GENERATION_ROUNDS_KEY, titleGenerationRounds)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {NInputNumber, NSwitch} from 'naive-ui'
import {Search, Sparkles} from '@lucide/vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {showToast} from '@/composables/useToast'

const SEARCH_RESULT_COUNT_KEY = 'SEARCH_RESULT_COUNT'
const WEB_FETCH_MAX_CHARS_KEY = 'WEB_FETCH_MAX_CHARS'
const ENABLE_AUTO_TITLE_KEY = 'ENABLE_AUTO_TITLE'
const TITLE_GENERATION_ROUNDS_KEY = 'TITLE_GENERATION_ROUNDS'

const searchResultCount = ref<number | null>(null)
const webFetchMaxChars = ref<number | null>(null)
const enableAutoTitle = ref(true)
const titleGenerationRounds = ref<number | null>(1)

/** 缓存初始全量配置，用于 blur 时合并其他未变化字段 */
const initialConfigs = ref<Record<string, string>>({})

onMounted(async () => {
  try {
    const configs = await fetchConfigs()
    initialConfigs.value = {...configs}

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

    const eatRaw = configs[ENABLE_AUTO_TITLE_KEY]
    if (eatRaw && eatRaw !== '{}') {
      enableAutoTitle.value = eatRaw === 'true'
    }

    const tgrRaw = configs[TITLE_GENERATION_ROUNDS_KEY]
    if (tgrRaw && tgrRaw !== '{}') {
      const n = parseInt(tgrRaw, 10)
      if (!isNaN(n)) titleGenerationRounds.value = Math.min(Math.max(n, 1), 5)
    }
  } catch (err: any) {
    showToast('加载配置失败: ' + (err.message || '未知错误'), 'error')
  }
})

/**
 * 失去焦点时自动保存单个字段
 * 与初始配置合并后全量提交，避免覆盖其他未加载的配置
 */
async function saveField(key: string, value: number | boolean | null) {
  try {
    const payload: Record<string, string> = {
      ...initialConfigs.value,
    }
    if (value != null) {
      payload[key] = String(value)
    } else {
      delete payload[key]
    }
    await updateConfigs(payload)
    // 更新本地缓存，为下次 blur 做准备
    initialConfigs.value = payload
  } catch (err: any) {
    showToast('保存失败: ' + (err.message || '未知错误'), 'error')
  }
}

/** 自动标题开关变更立即保存 */
async function onAutoTitleChange(val: boolean) {
  enableAutoTitle.value = val
  await saveField(ENABLE_AUTO_TITLE_KEY, val)
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
</style>
