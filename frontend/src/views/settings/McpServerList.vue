<template>
  <div class="mcp-card-list">
    <div v-if="loading" class="mcp-loading">加载中...</div>

    <template v-else>
      <div
          v-for="server in servers"
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
            <NIcon :size="18" class="mcp-card-icon">
              <Network/>
            </NIcon>
            <NIcon :size="14" class="chevron-icon" :class="{ rotated: expandedId === server.id }">
              <ChevronDown/>
            </NIcon>
            <span class="mcp-card-name" :class="{ disabled: !server.enabled }">
              {{ server.name }}
            </span>
            <n-tag :bordered="false" size="tiny" :type="tagType(server.transportType)">
              {{ transportLabel(server.transportType) }}
            </n-tag>
            <n-tag v-if="server.tools && server.tools.length > 0" :bordered="false" size="tiny"
                   :type="server.enabled ? 'success' : 'default'">
              {{ server.tools.length }} 个工具
            </n-tag>
          </div>

          <div class="mcp-card-actions">
            <label class="sidebar-toggle" @click.stop="toggleEnabled(server)">
              <input
                  type="checkbox"
                  :checked="server.enabled"
                  @click.stop
                  @change.stop
              />
              <span class="toggle-track"></span>
            </label>

            <n-button
                size="tiny"
                quaternary
                :disabled="server.sourceType !== 'USER'"
                :title="server.sourceType !== 'USER' ? '系统预置不可编辑' : '编辑'"
                @click.stop="$emit('edit', server)"
            >
              <template #icon>
                <NIcon>
                  <EditIcon/>
                </NIcon>
              </template>
            </n-button>

            <n-button
                size="tiny"
                quaternary
                :disabled="server.sourceType !== 'USER'"
                :title="server.sourceType !== 'USER' ? '系统预置不可删除' : '删除'"
                @click.stop="$emit('delete', server)"
            >
              <template #icon>
                <NIcon>
                  <Trash2/>
                </NIcon>
              </template>
            </n-button>
          </div>
        </div>

        <!-- 展开详情：工具列表 -->
        <div v-if="expandedId === server.id" class="mcp-server-detail">
          <div v-if="server.description" class="detail-row">
            <span class="detail-label">描述</span>
            <span class="detail-value">{{ server.description }}</span>
          </div>

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
                  <span class="tool-name" :title="tool.name">{{ tool.name }}</span>
                  <div v-if="tool.description" class="tool-desc">{{ tool.description }}</div>
                  <div class="tool-meta">
                    <code>{{ tool.name }}</code>
                  </div>
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
            </div>
            <div v-if="!server.tools || server.tools.length === 0" class="tools-empty">
              暂无工具，启用后自动同步
            </div>
          </div>
        </div>
      </div>

      <div v-if="servers.length === 0" class="mcp-empty">
        <NIcon :size="40" class="mcp-empty-icon">
          <Network/>
        </NIcon>
        <span>暂无 MCP 服务，点击右上角「新增服务器」添加</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {NButton, NIcon, NTag} from 'naive-ui'
import {ChevronDown, Edit as EditIcon, Network, Trash2} from '@lucide/vue'
import type {McpServer, McpToolItem} from '@/types/mcpServer'
import {TRANSPORT_LABELS} from '@/types/mcpServer'

const props = defineProps<{
  servers: McpServer[]
  loading: boolean
}>()

const emit = defineEmits<{
  toggle: [server: McpServer]
  edit: [server: McpServer]
  delete: [server: McpServer]
  toggleTool: [server: McpServer, tool: McpToolItem]
}>()

const expandedId = ref<number | null>(null)

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function toggleEnabled(server: McpServer) {
  emit('toggle', server)
}

function handleToolToggle(server: McpServer, tool: McpToolItem) {
  emit('toggleTool', server, tool)
}

function transportLabel(type: string): string {
  return TRANSPORT_LABELS[type] || type
}

function tagType(type: string): 'success' | 'info' | 'warning' | 'default' {
  switch (type) {
    case 'STDIO':
      return 'success'
    case 'SSE':
      return 'info'
    case 'STREAMABLE_HTTP':
      return 'warning'
    default:
      return 'default'
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
.mcp-server-detail {
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

.tool-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 50ch;
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

.tools-empty {
  font-size: var(--font-size-small);
  color: var(--text-quaternary);
  padding: 12px 0;
  text-align: center;
}

.mcp-card-header {
  cursor: pointer;
}

/* ===== 自定义 Toggle 开关（同内置工具统一大小，em 单位随全局字号缩放） ===== */
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

/* ===== 编辑/删除按钮图标：使用 --icon-size CSS 变量，颜色显式设置 ===== */
.mcp-card-actions :deep(.n-button .n-icon) {
  font-size: var(--icon-size-sm);
  color: var(--text-tertiary);
}

.mcp-card-actions :deep(.n-button:hover .n-icon) {
  color: var(--text-primary);
}

.mcp-card-actions :deep(.n-button--disabled .n-icon) {
  color: var(--text-quaternary);
}
</style>
