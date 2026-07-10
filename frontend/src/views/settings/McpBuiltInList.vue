<!--
 * @author Eddie
 * @date 2026-06-26
-->

<template>
  <div class="mcp-card-list">
    <div v-if="loading" class="mcp-loading">加载中...</div>

    <template v-else>
      <div
          v-for="server in builtInServers"
          :key="server.id"
          class="mcp-card"
          :class="{ expanded: expandedId === server.id }"
      >
        <!-- 卡片头部 -->
        <div class="mcp-card-header" @click="toggleExpand(server.id)">
          <div class="mcp-card-left">
            <!-- 连接状态圆点（仅启用时展示） -->
            <span v-if="server.enabled"
                  class="status-dot"
                  :class="{
                    'connected': server.connectionStatus === 'CONNECTED',
                    'disconnected': server.connectionStatus !== 'CONNECTED',
                  }"
            />
            <HardDrive :size="18" :stroke-width="1.5" class="mcp-card-icon"/>
            <ChevronDown
                :size="14"
                :stroke-width="2.5"
                class="chevron-icon"
                :class="{ rotated: expandedId === server.id }"
            />
            <span class="mcp-card-name" :class="{ disabled: !server.enabled }">
              {{ server.name }}
            </span>
            <span class="mcp-tag built-in">内置</span>
            <span class="mcp-tag" :class="server.enabled ? 'enabled' : 'disabled'">
              {{ server.tools.length }} 个工具
            </span>
            <span v-if="showPartialLabel(server)" class="mcp-tag partial">
              已启用 {{ enabledToolCount(server) }}
            </span>
          </div>

          <div class="mcp-card-actions">
            <label class="sidebar-toggle mcp-toggle" @click.stop="handleToggle(server)">
              <input
                  type="checkbox"
                  :checked="server.enabled"
                  @click.stop
                  @change.stop
              />
              <span class="toggle-track"></span>
            </label>
          </div>
        </div>

        <!-- 展开详情：工具列表 + 各工具配置面板 -->
        <div v-if="expandedId === server.id" class="mcp-builtin-detail">
          <div v-if="server.url" class="detail-row">
            <span class="detail-label">端点</span>
            <code class="detail-value">{{ server.url }}</code>
          </div>

          <div class="tools-section">
            <div class="tools-title">工具列表</div>
            <div
                v-for="tool in server.tools"
                :key="tool.id"
                class="tool-item"
            >
              <div class="tool-header">
                <div class="tool-info">
                  <span class="tool-name">{{ tool.name }}</span>
                  <div v-if="tool.description" class="tool-desc markdown-body" v-html="renderMd(tool.description)" />
                </div>
                <label class="sidebar-toggle tool-toggle" @click.prevent="handleToolToggle(server, tool)">
                  <input
                      type="checkbox"
                      :checked="tool.enabled"
                      @click.stop
                      @change.stop
                  />
                  <span class="toggle-track"></span>
                </label>
              </div>

              <!-- 该工具的配置面板（仅支持配置的内置工具） -->
              <div v-if="tool.configSchema?.fields?.length" class="config-section">
                <div class="config-title">{{ tool.configSchema.title }}</div>
                <div class="config-desc">{{ tool.configSchema.description }}</div>

                <div
                    v-for="field in tool.configSchema.fields"
                    :key="field.name"
                    class="config-field"
                    v-show="!field.dependsOn || getFieldValue(server, tool.name, field.dependsOn) === field.dependsOnValue"
                >
                  <label class="config-field-label">{{ field.label }}</label>
                  <span class="config-field-hint">{{ field.description }}</span>

                  <!-- select 类型 -->
                  <n-select
                      v-if="field.type === 'select'"
                      :value="getFieldValue(server, tool.name, field.name)"
                      :options="field.options"
                      class="config-input"
                      @update:value="(val: string) => { setFieldValue(server, tool.name, field.name, val); saveConfig(server, tool.name) }"
                  />

                  <!-- textarea 类型 -->
                  <n-input
                      v-else-if="field.type === 'textarea'"
                      :value="getFieldValue(server, tool.name, field.name)"
                      type="textarea"
                      :rows="5"
                      class="config-input"
                      @update:value="(val: string) => setFieldValue(server, tool.name, field.name, val)"
                      @blur="saveConfig(server, tool.name)"
                  />

                  <!-- string 类型 -->
                  <n-input
                      v-else-if="field.type === 'string'"
                      :value="getFieldValue(server, tool.name, field.name)"
                      :placeholder="field.placeholder"
                      class="config-input"
                      @update:value="(val: string) => setFieldValue(server, tool.name, field.name, val)"
                      @blur="saveConfig(server, tool.name)"
                  />

                  <!-- number 类型 -->
                  <n-input-number
                      v-else-if="field.type === 'number'"
                      :value="Number(getFieldValue(server, tool.name, field.name))"
                      :min="field.min"
                      :max="field.max"
                      class="config-input-number"
                      @update:value="(val: number | null) => setFieldValue(server, tool.name, field.name, val)"
                      @blur="saveConfig(server, tool.name)"
                  />

                  <!-- boolean 类型 -->
                  <n-switch
                      v-else-if="field.type === 'boolean'"
                      :value="getFieldValue(server, tool.name, field.name) === true || getFieldValue(server, tool.name, field.name) === 'true'"
                      @update:value="(val: boolean) => { setFieldValue(server, tool.name, field.name, val); saveConfig(server, tool.name) }"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="builtInServers.length === 0" class="mcp-empty">
        <HardDrive :size="40" :stroke-width="1" class="mcp-empty-icon"/>
        <span>暂无内置工具</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {NInput, NInputNumber, NSelect, NSwitch} from 'naive-ui'
import {ChevronDown, HardDrive} from '@lucide/vue'
import type {McpServer, McpToolItem} from '@/types/mcpServer'
import {updateMcpServer} from '@/api/mcpServer'
import {renderMd} from '@/utils/markdown'
import {showToast} from '@/composables/useToast'

const props = defineProps<{
  servers: McpServer[]
  loading: boolean
}>()

const emit = defineEmits<{
  toggle: [server: McpServer]
  toggleTool: [server: McpServer, tool: McpToolItem]
}>()

const expandedId = ref<number | null>(null)

/**
 * 本地配置缓存：serverId → toolName → { fieldName: value }
 * 用于在不触发父组件重渲染的情况下缓存编辑中的配置值
 */
const configCache = reactive<Record<number, Record<string, Record<string, any>>>>({})

const builtInServers = computed(() =>
    props.servers.filter(s => s.sourceType === 'BUILT_IN')
)

function enabledToolCount(server: McpServer): number {
  return server.tools.filter(t => t.enabled).length
}

function handleToolToggle(server: McpServer, tool: McpToolItem) {
  emit('toggleTool', server, tool)
}

function showPartialLabel(server: McpServer): boolean {
  if (!server.enabled || !server.tools?.length) return false
  const count = enabledToolCount(server)
  return count > 0 && count < server.tools.length
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function handleToggle(server: McpServer) {
  emit('toggle', server)
}

// ===== 配置管理（按 tool name 隔离） =====

/**
 * 获取指定工具在 source_config 中的配置段（合并默认值）。
 */
function getToolConfigObj(server: McpServer, toolName: string): Record<string, any> {
  // 先从本地缓存读取
  if (configCache[server.id]?.[toolName]) {
    return configCache[server.id][toolName]
  }

  // 从服务器返回的 sourceConfig 解析
  const config: Record<string, any> = {}
  if (server.sourceConfig && server.sourceConfig !== '{}') {
    try {
      const parsed = JSON.parse(server.sourceConfig)
      // 取该工具的配置段（新格式），若没有则当旧格式扁平处理
      const toolConfig = parsed[toolName]
      if (toolConfig && typeof toolConfig === 'object') {
        Object.assign(config, toolConfig)
      } else {
        // 旧格式兼容：整个 sourceConfig 就是该工具的配置
        Object.assign(config, parsed)
      }
    } catch { /* ignore */ }
  }

  // 合并默认值
  const tool = server.tools.find(t => t.name === toolName)
  if (tool?.configSchema?.fields) {
    for (const field of tool.configSchema.fields) {
      if (!(field.name in config) && field.defaultValue !== undefined) {
        config[field.name] = field.defaultValue
      }
    }
  }

  if (!configCache[server.id]) {
    configCache[server.id] = {}
  }
  configCache[server.id][toolName] = config
  return config
}

/**
 * 根据字段名获取当前值（用于条件可见性判断）
 */
function getFieldValue(server: McpServer, toolName: string, fieldName: string): any {
  const config = getToolConfigObj(server, toolName)
  const val = config[fieldName]
  if (val !== undefined) return val
  // 找不到则从 schema 默认值查找
  const tool = server.tools.find(t => t.name === toolName)
  const field = tool?.configSchema?.fields?.find(f => f.name === fieldName)
  return field?.defaultValue
}

/**
 * 设置单个字段的值
 */
function setFieldValue(server: McpServer, toolName: string, fieldName: string, value: any) {
  const config = getToolConfigObj(server, toolName)
  config[fieldName] = value
  if (!configCache[server.id]) {
    configCache[server.id] = {}
  }
  configCache[server.id][toolName] = config
}

/**
 * 保存指定工具的配置到后端（按 tool name namespace 合并写入）
 */
async function saveConfig(server: McpServer, toolName: string) {
  const toolConfig = configCache[server.id]?.[toolName]
  if (!toolConfig) return

  // 读取当前完整 sourceConfig
  let full: Record<string, any> = {}
  if (server.sourceConfig && server.sourceConfig !== '{}') {
    try { full = JSON.parse(server.sourceConfig) } catch { /* ignore */ }
  }
  // 仅更新该工具的配置段
  full[toolName] = toolConfig

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
</script>

<style scoped src="./mcp-server.css"></style>
<style scoped>
/* ===== 展开箭头 ===== */
.chevron-icon {
  flex-shrink: 0;
  color: var(--text-quaternary);
  transition: transform 0.2s, color 0.15s;
  margin-right: 4px;
}

.mcp-card-header:hover .chevron-icon {
  color: var(--text-secondary);
}

.chevron-icon.rotated {
  transform: rotate(180deg);
}

/* ===== 展开详情 ===== */
.mcp-builtin-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-default);
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: var(--font-size-small);
}

.detail-label {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.detail-value {
  font-size: var(--font-size-small);
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
  color: var(--text-secondary);
  word-break: break-all;
}

/* ===== 工具列表 ===== */
.tools-section {
  margin-top: 4px;
}

.tools-title {
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.tool-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--bg-hover);
  margin-bottom: 6px;
}

.tool-item:last-child {
  margin-bottom: 0;
}

.tool-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tool-info {
  flex: 1;
  min-width: 0;
}

.tool-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
}

.tool-desc {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  margin-top: 4px;
  line-height: 1.5;
}

.tool-meta {
  margin-top: 4px;
}

/* ===== Markdown 渲染（在工具详情卡片内） ===== */
.tool-desc.markdown-body {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.6;
}
.tool-desc.markdown-body h1,
.tool-desc.markdown-body h2,
.tool-desc.markdown-body h3,
.tool-desc.markdown-body h4 {
  margin: 0.3em 0 0.15em;
  font-size: 1em;
  font-weight: 600;
}
.tool-desc.markdown-body p {
  margin: 0.2em 0;
}
.tool-desc.markdown-body ul,
.tool-desc.markdown-body ol {
  padding-left: 1.2em;
  margin: 0.2em 0;
}
.tool-desc.markdown-body li {
  margin: 0.1em 0;
}
.tool-desc.markdown-body code {
  font-size: 0.92em;
  padding: 0.1em 0.3em;
}
.tool-desc.markdown-body pre {
  margin: 0.4em 0;
  padding: 8px 10px;
  border-radius: 6px;
}


/* ===== 部分启用标签 ===== */
.mcp-tag.partial {
  background: var(--tag-warning-bg, #fef3cd);
  color: var(--tag-warning-text, #856404);
}

/* ===== Toggle 开关（em 单位，随全局字号缩放） ===== */
.sidebar-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 2em;
  height: 1.125em;
  font-size: var(--font-size-base);
  cursor: pointer;
  flex-shrink: 0;
}

.sidebar-toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.sidebar-toggle .toggle-track {
  position: absolute;
  inset: 0;
  background: var(--border-hover);
  border-radius: 9px;
  transition: background 0.2s;
}

.sidebar-toggle .toggle-track::before {
  content: '';
  position: absolute;
  top: 0.125em;
  left: 0.125em;
  width: 0.875em;
  height: 0.875em;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.sidebar-toggle input:checked + .toggle-track {
  background: var(--accent-default);
}

.sidebar-toggle input:checked + .toggle-track::before {
  transform: translateX(0.875em);
}

.tool-toggle {
  margin-top: var(--space-0, 0.125em);
}

.mcp-card-header {
  cursor: pointer;
}

/* ===== 配置面板 ===== */
.config-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-default);
}

.config-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.config-desc {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  margin-bottom: 16px;
  line-height: 1.5;
}

.config-field {
  margin-bottom: 14px;
}

.config-field-label {
  display: block;
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.config-field-hint {
  display: block;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  margin-bottom: 6px;
  line-height: 1.4;
}

.config-input {
  width: 100%;
}

.config-input-number {
  width: 120px;
}

/* ===== 连接状态圆点 ===== */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-right: 2px;
}

.status-dot.connected {
  background: var(--color-success, #52c41a);
  box-shadow: 0 0 6px rgba(82, 196, 26, 0.5);
}

.status-dot.disconnected {
  background: var(--color-warning, #faad14);
  box-shadow: 0 0 6px rgba(250, 173, 20, 0.5);
}

</style>
