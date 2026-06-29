<template>
  <div class="mcp-card-list">
    <div v-if="loading" class="mcp-loading">加载中...</div>

    <template v-else>
      <div v-for="server in servers" :key="server.id" class="mcp-card">
        <div class="mcp-card-header">
          <div class="mcp-card-left">
            <Network :size="18" :stroke-width="1.5" class="mcp-card-icon"/>
            <span class="mcp-card-name" :class="{ disabled: !server.enabled }">
              {{ server.name }}
            </span>
            <span class="mcp-tag" :class="transportClass(server.transportType)">
              {{ transportLabel(server.transportType) }}
            </span>
            <span v-if="server.tools && server.tools.length > 0" class="mcp-tag"
                  :class="server.enabled ? 'enabled' : 'disabled'">
              {{ server.tools.length }} 个工具
            </span>
          </div>

          <div class="mcp-card-actions">
            <!-- 启用/禁用开关 -->
            <label class="sidebar-toggle mcp-toggle" @click.prevent="toggleEnabled(server)">
              <input
                  type="checkbox"
                  :checked="server.enabled"
                  @click.stop
                  @change.stop
              />
              <span class="toggle-track"></span>
            </label>

            <!-- 编辑按钮 -->
            <button
                class="mcp-card-action-btn"
                :disabled="server.sourceType !== 'USER'"
                :title="server.sourceType !== 'USER' ? '系统预置不可编辑' : '编辑'"
                @click="$emit('edit', server)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
              </svg>
            </button>

            <!-- 删除按钮 -->
            <button
                class="mcp-card-action-btn danger"
                :disabled="server.sourceType !== 'USER'"
                :title="server.sourceType !== 'USER' ? '系统预置不可删除' : '删除'"
                @click="$emit('delete', server)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18"/>
                <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-if="servers.length === 0" class="mcp-empty">
        <Network :size="40" :stroke-width="1" class="mcp-empty-icon"/>
        <span>暂无 MCP 服务，点击左上角「新增服务器」添加</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {Network} from '@lucide/vue'
import type {McpServer} from '@/types/mcpServer'
import {TRANSPORT_CLASSES, TRANSPORT_LABELS} from '@/types/mcpServer'

defineProps<{
  servers: McpServer[]
  loading: boolean
}>()

defineEmits<{
  toggle: [server: McpServer]
  edit: [server: McpServer]
  delete: [server: McpServer]
}>()

function transportLabel(type: string): string {
  return TRANSPORT_LABELS[type] || type
}

function transportClass(type: string): string {
  return TRANSPORT_CLASSES[type] || ''
}
</script>
