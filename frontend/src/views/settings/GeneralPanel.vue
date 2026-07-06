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
            @blur="saveField('searchResultCount', searchResultCount)"
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
            @blur="saveField('webFetchMaxChars', webFetchMaxChars)"
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
            @blur="saveField('titleGenerationRounds', titleGenerationRounds)"
        />
      </div>
    </div>

    <!-- ===== 日志设置 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Terminal :size="16" :stroke-width="2" class="group-icon"/>
        日志设置
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">业务日志级别</span>
          <span class="setting-hint">控制 cc.wlizhi.eddie 包及子包的日志输出，实时生效</span>
        </div>
        <n-select
            :value="logLevel"
            :options="logLevelOptions"
            style="width: 120px"
            @update:value="onLogLevelChange"
        />
      </div>
    </div>

    <!-- ===== 工具调用设置 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Wrench :size="16" :stroke-width="2" class="group-icon"/>
        工具调用设置
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">工具结果模型上下文最大长度</span>
          <span class="setting-hint">工具执行结果返回给 AI 模型的最大字符数，超出部分将被截断（0=不截断，范围 0~100,000）</span>
        </div>
        <n-input-number
            v-model:value="toolResultModelMaxLength"
            :min="0"
            :max="100000"
            :step="1000"
            :show-button="false"
            class="number-input"
            placeholder="20000"
            @update:value="onToolResultModelMaxLengthChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {NInputNumber, NSelect, NSwitch} from 'naive-ui'
import {Search, Sparkles, Terminal, Wrench} from '@lucide/vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {showToast} from '@/composables/useToast'

const GENERAL_SETTINGS_KEY = 'GENERAL_SETTINGS'

const searchResultCount = ref<number | null>(null)
const webFetchMaxChars = ref<number | null>(null)
const enableAutoTitle = ref(true)
const titleGenerationRounds = ref<number | null>(1)
const logLevel = ref('')
const logLevelOptions = [
  {label: '默认 (INFO)', value: ''},
  {label: 'TRACE', value: 'TRACE'},
  {label: 'DEBUG', value: 'DEBUG'},
  {label: 'INFO', value: 'INFO'},
  {label: 'WARN', value: 'WARN'},
  {label: 'ERROR', value: 'ERROR'},
  {label: 'OFF', value: 'OFF'},
]

const TOOL_RESULT_MODEL_MAX_LENGTH_KEY = 'TOOL_RESULT_MODEL_MAX_LENGTH'
const toolResultModelMaxLength = ref<number>(20000)

onMounted(async () => {
  try {
    const configs = await fetchConfigs()
    const raw = configs[GENERAL_SETTINGS_KEY]
    const settings = raw ? JSON.parse(raw) : {}

    if (settings.searchResultCount != null) {
      searchResultCount.value = Math.min(Math.max(settings.searchResultCount, 1), 20)
    }
    if (settings.webFetchMaxChars != null) {
      webFetchMaxChars.value = Math.min(Math.max(settings.webFetchMaxChars, 1000), 15000)
    }
    if (settings.enableAutoTitle != null) {
      enableAutoTitle.value = settings.enableAutoTitle === true
    }
    if (settings.titleGenerationRounds != null) {
      titleGenerationRounds.value = Math.min(Math.max(settings.titleGenerationRounds, 1), 5)
    }
    if (settings.logLevel != null) {
      logLevel.value = settings.logLevel
    }
    if (configs[TOOL_RESULT_MODEL_MAX_LENGTH_KEY] != null) {
      const val = parseInt(configs[TOOL_RESULT_MODEL_MAX_LENGTH_KEY], 10)
      if (!isNaN(val)) {
        toolResultModelMaxLength.value = Math.min(Math.max(val, 0), 100000)
      }
    }
  } catch (err: any) {
    showToast('加载配置失败: ' + (err.message || '未知错误'), 'error')
  }
})

/**
 * 失去焦点时自动保存单个字段
 * 更新 GENERAL_SETTINGS JSON 中的对应字段后全量提交
 */
async function saveField(key: string, value: number | boolean | null) {
  try {
    const configs = await fetchConfigs()
    const raw = configs[GENERAL_SETTINGS_KEY]
    const settings: Record<string, any> = raw ? JSON.parse(raw) : {}

    if (value != null) {
      settings[key] = value
    } else {
      delete settings[key]
    }
    await updateConfigs({[GENERAL_SETTINGS_KEY]: JSON.stringify(settings)})
  } catch (err: any) {
    showToast('保存失败: ' + (err.message || '未知错误'), 'error')
  }
}

/** 自动标题开关变更立即保存 */
async function onAutoTitleChange(val: boolean) {
  enableAutoTitle.value = val
  await saveField('enableAutoTitle', val)
}

/** 日志级别变更立即保存 */
function onLogLevelChange(val: string) {
  logLevel.value = val
  // 从本地 ref 构造完整的 GENERAL_SETTINGS JSON，避免先 GET 再 PUT
  const settings: Record<string, any> = {
    searchResultCount: searchResultCount.value,
    webFetchMaxChars: webFetchMaxChars.value,
    enableAutoTitle: enableAutoTitle.value,
    titleGenerationRounds: titleGenerationRounds.value,
    logLevel: val,
  }
  Object.keys(settings).forEach(k => {
    if (settings[k] == null) delete settings[k]
  })
  updateConfigs({[GENERAL_SETTINGS_KEY]: JSON.stringify(settings)})
      .catch(err => showToast('保存失败: ' + (err.message || '未知错误'), 'error'))
}

/** 工具结果模型上下文最大长度变更立即保存 */
async function onToolResultModelMaxLengthChange(val: number | null) {
  if (val === undefined || val === null) return
  const clamped = Math.min(Math.max(val, 0), 100000)
  toolResultModelMaxLength.value = clamped
  try {
    await updateConfigs({[TOOL_RESULT_MODEL_MAX_LENGTH_KEY]: String(clamped)})
  } catch (err: any) {
    showToast('保存失败: ' + (err.message || '未知错误'), 'error')
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
</style>
