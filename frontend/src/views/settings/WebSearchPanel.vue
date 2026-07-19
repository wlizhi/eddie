<!--
 * @author Eddie
 * @date 2026-07-19
 -->

<template>
  <div class="panel web-search-panel">
    <!-- ===== 内置搜索引擎 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Search :size="16" :stroke-width="2" class="group-icon"/>
        内置搜索引擎
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">启用内置搜索</span>
          <span class="setting-hint">开启后 AI 可使用内置搜索引擎获取实时信息</span>
        </div>
        <n-switch
            :value="searchEnabled"
            :loading="toggleLoading"
            @update:value="onToggleSearch"
        />
      </div>

      <template v-if="searchEnabled">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">默认搜索引擎</span>
            <span class="setting-hint">AI 未指定搜索引擎时默认使用的引擎</span>
          </div>
          <n-select
              :value="searchEngine"
              :options="engineOptions"
              :disabled="!builtInServer"
              style="width: 14em"
              @update:value="onEngineChange"
          />
        </div>

        <div class="setting-row" v-if="searchEngine === 'TAVILY'">
          <div class="setting-info">
            <span class="setting-label">Tavily API Key</span>
            <span class="setting-hint">
              从
              <a href="https://app.tavily.com/home" target="_blank" rel="noopener" class="external-link">app.tavily.com</a>
              注册获取
            </span>
          </div>
          <n-input
              :value="tavilyApiKey"
              type="password"
              show-password-on="click"
              :placeholder="'输入 API Key'"
              :disabled="!builtInServer"
              style="width: 14em"
              @update:value="onTavilyKeyChange"
              @blur="saveServerConfig"
          />
        </div>
      </template>
    </div>

    <!-- ===== 搜索参数 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Settings2 :size="16" :stroke-width="2" class="group-icon"/>
        搜索参数
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
            @blur="saveSearchParam('searchResultCount', searchResultCount)"
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
            @blur="saveSearchParam('webFetchMaxChars', webFetchMaxChars)"
        />
      </div>
    </div>

    <!-- ===== 高级选项：MCP 搜索服务器 ===== -->
    <div class="mcp-hint-card" @click="goToMcp">
      <div class="mcp-hint-content">
        <Network :size="20" :stroke-width="1.5" class="mcp-hint-icon"/>
        <div class="mcp-hint-text">
          <span class="mcp-hint-title">需要更强大的搜索引擎？</span>
          <span class="mcp-hint-desc">可通过 MCP 服务添加 SearXNG、Tavily 等自定义搜索引擎服务器。</span>
        </div>
      </div>
      <span class="mcp-hint-link">前往 MCP 服务 →</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {inject, onMounted, ref} from 'vue'
import {NInput, NInputNumber, NSelect, NSwitch} from 'naive-ui'
import {Network, Search, Settings2} from '@lucide/vue'
import {listMcpServers, updateBuiltInToolStatus, updateMcpServer} from '@/api/mcpServer'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {showToast} from '@/composables/useToast'
import type {McpServer} from '@/types/mcpServer'

const GENERAL_SETTINGS_KEY = 'GENERAL_SETTINGS'
const BUILT_IN_SEARCH_NAME = 'BuiltInSearch'

/** 从 SettingsView 注入的导航函数 */
const navigateTo = inject<(key: string) => void>('navigateTo')

/** 内置搜索服务器数据 */
const builtInServer = ref<McpServer | null>(null)
const toggleLoading = ref(false)
const searchEnabled = ref(false)

/** 搜索引擎配置 */
const searchEngine = ref('DUCKDUCKGO')
const tavilyApiKey = ref('')

/** 搜索参数 */
const searchResultCount = ref<number | null>(null)
const webFetchMaxChars = ref<number | null>(null)

const engineOptions = [
  {value: 'DUCKDUCKGO', label: 'DuckDuckGo（默认）'},
  {value: 'BING', label: 'Bing（国内直连）'},
  {value: 'TAVILY', label: 'Tavily（需 API Key）'},
]

onMounted(async () => {
  try {
    await loadData()
  } catch (err: any) {
    showToast('加载失败: ' + (err.message || '未知错误'), 'error')
  }
})

async function loadData() {
  // 1. 加载内置搜索配置
  const servers = await listMcpServers()
  const builtIn = servers.find(s => s.name === BUILT_IN_SEARCH_NAME) || null
  builtInServer.value = builtIn

  if (builtIn) {
    searchEnabled.value = builtIn.enabled

    // 解析 server 级配置
    if (builtIn.sourceConfig && builtIn.sourceConfig !== '{}') {
      try {
        const parsed = JSON.parse(builtIn.sourceConfig)
        const serverCfg = parsed['server']
        if (serverCfg) {
          if (serverCfg.engine) searchEngine.value = serverCfg.engine
          if (serverCfg.tavilyApiKey) tavilyApiKey.value = serverCfg.tavilyApiKey
        }
      } catch { /* ignore */ }
    }
  }

  // 2. 加载全局搜索参数
  const configs = await fetchConfigs()
  const raw = configs[GENERAL_SETTINGS_KEY]
  const settings = raw ? JSON.parse(raw) : {}

  if (settings.searchResultCount != null) {
    searchResultCount.value = Math.min(Math.max(settings.searchResultCount, 1), 20)
  }
  if (settings.webFetchMaxChars != null) {
    webFetchMaxChars.value = Math.min(Math.max(settings.webFetchMaxChars, 1000), 15000)
  }
}

/** 启用/禁用内置搜索 */
async function onToggleSearch(val: boolean) {
  if (!builtInServer.value) return
  toggleLoading.value = true
  try {
    await updateBuiltInToolStatus({mcpServerId: builtInServer.value.id, enabled: val})
    searchEnabled.value = val
    showToast(val ? '内置搜索已启用' : '内置搜索已禁用')
  } catch (err: any) {
    showToast('操作失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    toggleLoading.value = false
  }
}

/** 修改默认搜索引擎 — 即时保存 */
async function onEngineChange(val: string) {
  searchEngine.value = val
  await saveServerConfig()
}

/** 修改 Tavily API Key — 即时保存 */
async function onTavilyKeyChange(val: string) {
  tavilyApiKey.value = val
}

/** 保存内置搜索的 Server 级配置（engine / tavilyApiKey） */
async function saveServerConfig() {
  const server = builtInServer.value
  if (!server) return

  const serverConfig: Record<string, any> = {
    engine: searchEngine.value,
  }
  if (tavilyApiKey.value) {
    serverConfig.tavilyApiKey = tavilyApiKey.value
  }

  // 读取当前完整 sourceConfig，保留其他命名空间段
  let full: Record<string, any> = {}
  if (server.sourceConfig && server.sourceConfig !== '{}') {
    try {
      const parsed = JSON.parse(server.sourceConfig)
      for (const [key, val] of Object.entries(parsed)) {
        if (val && typeof val === 'object' && !Array.isArray(val)) {
          full[key] = val
        }
      }
    } catch { /* ignore */ }
  }
  full['server'] = serverConfig

  try {
    await updateMcpServer(server.id, {
      name: server.name,
      sourceType: 'BUILT_IN',
      sourceConfig: JSON.stringify(full),
      transportType: 'BUILT_IN',
    })
    showToast('配置已保存', 'success')
  } catch (err: any) {
    showToast('保存配置失败: ' + (err.message || '未知错误'), 'error')
  }
}

/** 跳转到 MCP 服务设置页 */
function goToMcp() {
  navigateTo?.('mcp')
}

/** 保存搜索参数到 GENERAL_SETTINGS */
async function saveSearchParam(key: string, value: number | null) {
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
</script>

<style scoped>
.web-search-panel {
  max-width: 40rem;
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
  width: 7.5em;
}

.external-link {
  color: var(--accent-default);
  text-decoration: none;
}

.external-link:hover {
  text-decoration: underline;
}

/* ===== MCP 引导卡片 ===== */
.mcp-hint-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 14px 18px;
  border: 1px solid var(--border-default);
  border-radius: 10px;
  background: var(--bg-secondary);
  cursor: pointer;
}

.mcp-hint-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.mcp-hint-icon {
  flex-shrink: 0;
  color: var(--text-tertiary);
  margin-top: 1px;
}

.mcp-hint-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.mcp-hint-title {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
}

.mcp-hint-desc {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.4;
}

.mcp-hint-link {
  flex-shrink: 0;
  font-size: var(--font-size-small);
  color: var(--accent-default);
  font-weight: 500;
  white-space: nowrap;
  margin-left: 12px;
}
</style>
