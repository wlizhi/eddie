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
            <span v-if="needsConfig(server)" class="mcp-tag needs-config">需要配置</span>
            <span class="mcp-tag" :class="server.enabled ? 'enabled' : 'disabled'">
              {{ server.tools.length }} 个工具
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

        <!-- 展开详情：工具列表 -->
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
                  <span class="tool-name">{{ tool.displayName || tool.name }}</span>
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
          </div>
        </div>
      </div>

      <div v-if="builtInServers.length === 0" class="mcp-empty">
        <HardDrive :size="40" :stroke-width="1" class="mcp-empty-icon"/>
        <span>暂无内置 MCP 服务</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import {ChevronDown, HardDrive} from '@lucide/vue'
import type {McpServer, McpToolItem} from '@/types/mcpServer'

const props = defineProps<{
  servers: McpServer[]
  loading: boolean
}>()

const emit = defineEmits<{
  toggle: [server: McpServer]
  toggleTool: [server: McpServer, tool: McpToolItem]
}>()

const expandedId = ref<number | null>(null)

const builtInServers = computed(() =>
    props.servers.filter(s => s.builtIn)
)

function needsConfig(server: McpServer): boolean {
  return !server.command && !server.url
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function handleToggle(server: McpServer) {
  emit('toggle', server)
}

function handleToolToggle(server: McpServer, tool: McpToolItem) {
  emit('toggleTool', server, tool)
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

.tool-meta code {
  font-size: var(--font-size-xs);
  background: var(--bg-secondary);
  padding: 1px 5px;
  border-radius: 3px;
  color: var(--text-quaternary);
}

/* ===== Toggle 开关 ===== */
.sidebar-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 32px;
  height: 18px;
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
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.sidebar-toggle input:checked + .toggle-track {
  background: var(--accent-default);
}

.sidebar-toggle input:checked + .toggle-track::before {
  transform: translateX(14px);
}

.tool-toggle {
  margin-top: 2px;
}

.mcp-card-header {
  cursor: pointer;
}

</style>
