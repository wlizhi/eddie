<!--
 * @author Eddie
 * @date 2026-06-22
-->

<template>
  <div class="mcp-layout">
    <McpSidebar
        :active-tab="activeTab"
        @update:active-tab="activeTab = $event"
    />

    <div class="mcp-content">
      <div class="mcp-content-header">
        <button v-if="activeTab === 'mcp'" class="mcp-add-btn" @click="openAddDialog">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新增服务器
        </button>
      </div>

      <McpServerList
          v-if="activeTab === 'mcp'"
          :servers="userServers"
          :loading="loading"
          @toggle="handleToggle"
          @edit="handleEdit"
          @delete="handleDelete"
          @toggle-tool="handleToolToggle"
      />

      <McpBuiltInList
          v-else-if="activeTab === 'builtin'"
          :servers="servers"
          :loading="loading"
          @toggle="handleToggle"
          @toggle-tool="handleToolToggle"
      />

      <McpDiscover v-else-if="activeTab === 'discover'"/>
    </div>

    <!-- 新增/编辑弹窗 -->
    <McpFormModal
        v-if="showForm"
        :editing="editingServer"
        @close="closeForm"
        @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useDialog} from 'naive-ui'
import {deleteMcpServer, listMcpServers, updateBuiltInToolStatus, updateMcpStatus} from '@/api/mcpServer'
import {showToast} from '@/composables/useToast'
import type {McpServer, McpToolItem} from '@/types/mcpServer'
import McpSidebar from './McpSidebar.vue'
import McpServerList from './McpServerList.vue'
import McpBuiltInList from './McpBuiltInList.vue'
import McpDiscover from './McpDiscover.vue'
import McpFormModal from './McpFormModal.vue'

const dialog = useDialog()
const loading = ref(false)
const servers = ref<McpServer[]>([])
const activeTab = ref<'builtin' | 'mcp' | 'discover'>('builtin')
const showForm = ref(false)
const editingServer = ref<McpServer | null>(null)

/** 用户自定义的 MCP 服务（USER 类型） */
const userServers = computed(() => servers.value.filter(s => s.sourceType === 'USER'))

/** 加载 MCP 列表 */
async function loadData() {
  loading.value = true
  try {
    servers.value = await listMcpServers()
  } catch (e) {
    const msg = (e as Error).message || '加载 MCP 列表失败'
    console.error('加载 MCP 列表失败:', msg)
    showToast(msg, 'error')
  } finally {
    loading.value = false
  }
}

/** 启用/禁用切换（按 sourceType 路由） */
async function handleToggle(server: McpServer) {
  const newEnabled = !server.enabled
  try {
    if (server.sourceType === 'BUILT_IN') {
      // 内置工具 MCP 级别切换：单次批量调用，后端更新所有工具 + 联动 MCP 状态
      await updateBuiltInToolStatus({mcpServerId: server.id, enabled: newEnabled})
      showToast(newEnabled ? '内置工具已启用' : '内置工具已禁用')
    } else {
      // MCP 扩展：不传 tools，后端自动级联
      const result = await updateMcpStatus({
        mcpServerId: server.id,
        mcpEnabled: newEnabled,
      })
      if (newEnabled && !result.connected) {
        showToast(result.message || '连接失败', 'error')
      } else {
        showToast(newEnabled ? 'MCP 服务已启用' : 'MCP 服务已禁用')
      }
    }
    await loadData()
  } catch (e) {
    const msg = (e as Error).message || '切换状态失败'
    console.error('切换状态失败:', msg)
    showToast(msg, 'error')
  }
}

/** 工具启用/禁用切换（按 sourceType 路由） */
async function handleToolToggle(server: McpServer, tool: McpToolItem) {
  const newEnabled = !tool.enabled
  try {
    if (server.sourceType === 'BUILT_IN') {
      // 内置工具工具级别切换：后端更新单个工具 + 自动联动 MCP 状态
      await updateBuiltInToolStatus({toolId: tool.id, enabled: newEnabled})
    } else {
      await updateMcpStatus({
        mcpServerId: server.id,
        tools: [{id: tool.id, enabled: newEnabled}],
      })
    }
    await loadData()
  } catch (e) {
    const msg = (e as Error).message || '切换工具状态失败'
    console.error('切换工具状态失败:', msg)
    showToast(msg, 'error')
  }
}

/** 编辑 MCP */
function handleEdit(server: McpServer) {
  editingServer.value = server
  showForm.value = true
}

/** 删除确认 */
function handleDelete(server: McpServer) {
  dialog.warning({
    title: '删除 MCP 服务',
    content: `确认删除 MCP 服务「${server.name}」？其下所有工具和绑定关系也将被删除。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteMcpServer(server.id)
        servers.value = servers.value.filter(s => s.id !== server.id)
        showToast('MCP 服务已删除')
      } catch (e) {
        const msg = (e as Error).message || '删除 MCP 服务失败'
        console.error('删除 MCP 服务失败:', msg)
        showToast(msg, 'error')
      }
    },
  })
}

/** 新建弹窗 */
function openAddDialog() {
  editingServer.value = null
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingServer.value = null
}

async function onSaved() {
  closeForm()
  await loadData()
}

onMounted(loadData)
</script>

<style scoped src="./mcp-server.css"></style>
