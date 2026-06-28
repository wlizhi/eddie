<!--
  McpPanelMobile.vue — 移动端 MCP 服务管理（精简版）

  功能：
  - 内置 MCP 服务和用户自定义服务统一列表展示
  - 每个服务显示名称、类型标签、工具数量
  - 启用/禁用切换
  - 无新增/编辑/删除功能（桌面端做）
-->
<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {Network} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import {listMcpServers, updateMcpStatus} from '@/api/mcpServer'
import type {McpServer} from '@/types/mcpServer'
import {TRANSPORT_CLASSES, TRANSPORT_LABELS} from '@/types/mcpServer'
import {showToast} from '@/composables/useToast'

const {iconSizeSm} = useIconSize()

const loading = ref(false)
const servers = ref<McpServer[]>([])

/** 已排序：内置服务在前，自定义在后 */
const sortedServers = computed(() => {
  const list = [...servers.value]
  list.sort((a, b) => {
    if (a.builtIn && !b.builtIn) return -1
    if (!a.builtIn && b.builtIn) return 1
    return 0
  })
  return list
})

async function loadData() {
  loading.value = true
  try {
    servers.value = await listMcpServers()
  } catch (e) {
    const msg = (e as Error).message || '加载失败'
    console.error('加载 MCP 列表失败:', msg)
    showToast(msg, 'error')
  } finally {
    loading.value = false
  }
}

/** 启用/禁用切换 */
async function toggleServer(server: McpServer) {
  const newEnabled = !server.enabled
  try {
    await updateMcpStatus({
      mcpServerId: server.id,
      mcpEnabled: newEnabled,
      tools: server.tools.map(t => ({id: t.id, enabled: newEnabled})),
    })
    await loadData()
  } catch (e) {
    const msg = (e as Error).message || '切换失败'
    showToast(msg, 'error')
  }
}

function transportLabel(type: string): string {
  return TRANSPORT_LABELS[type] || type
}

function transportClass(type: string): string {
  return TRANSPORT_CLASSES[type] || ''
}

onMounted(loadData)
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:var(--space-3)">
    <div v-if="loading"
         style="text-align:center;padding:var(--space-12);color:var(--text-tertiary);font-size:var(--font-size-base)">
      加载中...
    </div>

    <template v-else>
      <div
          v-for="server in sortedServers"
          :key="server.id"
          style="border:1px solid var(--border-default);border-radius:12px;padding:var(--space-4);background:var(--bg-secondary)"
      >
        <div style="display:flex;align-items:center;gap:var(--space-3)">
          <Network :size="iconSizeSm" :stroke-width="1.5"
                   style="flex-shrink:0;color:var(--accent-default)"/>
          <div style="flex:1;min-width:0">
            <div style="font-size:var(--font-size-base);font-weight:500;color:var(--text-primary)"
                 :style="{color: !server.enabled ? 'var(--text-tertiary)' : undefined}">
              {{ server.name }}
            </div>
            <div style="display:flex;align-items:center;gap:var(--space-2);margin-top:var(--space-1)">
                            <span class="mcp-tag-mobile" :class="transportClass(server.transportType)">
                                {{ transportLabel(server.transportType) }}
                            </span>
              <span v-if="server.builtIn" class="mcp-tag-mobile built-in">内置</span>
              <span v-if="server.tools?.length" style="font-size:var(--font-size-xs);color:var(--text-tertiary)">
                                {{ server.tools.length }} 工具
                            </span>
            </div>
          </div>
          <label style="flex-shrink:0;display:flex;align-items:center;cursor:pointer"
                 @click.prevent="toggleServer(server)">
            <input
                type="checkbox"
                :checked="server.enabled"
                style="display:none"
                @click.stop
                @change.stop
            />
            <span style="position:relative;display:inline-flex;width:2.25rem;height:1.25rem;
                             background:var(--border-hover);border-radius:10px;transition:background 0.2s"
                  :style="{background: server.enabled ? 'var(--accent-default)' : 'var(--border-hover)'}">
                            <span style="position:absolute;top:var(--space-1);left:var(--space-1);
                                 width:1rem;height:1rem;background:var(--bg-primary);border-radius:50%;
                                 transition:transform 0.2s;box-shadow:0 1px 3px var(--accent-ring)"
                                  :style="{transform: server.enabled ? 'translateX(1rem)' : 'translateX(0)'}"/>
                        </span>
          </label>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="servers.length === 0 && !loading"
           style="display:flex;flex-direction:column;align-items:center;gap:var(--space-3);padding:var(--space-12) 0;color:var(--text-tertiary)">
        <Network :size="40" :stroke-width="1" style="color:var(--icon-muted)"/>
        <span style="font-size:var(--font-size-base)">暂无 MCP 服务</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.mcp-tag-mobile {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 4px;
  font-size: var(--font-size-xs);
  font-weight: 500;
  line-height: 1.6;
}

.mcp-tag-mobile.stdio {
  background: var(--tag-fc-bg);
  color: var(--tag-fc-text);
}

.mcp-tag-mobile.sse {
  background: var(--tag-vision-bg);
  color: var(--tag-vision-text);
}

.mcp-tag-mobile.http {
  background: var(--tag-web-bg);
  color: var(--tag-web-text);
}

.mcp-tag-mobile.built-in {
  background: var(--tag-rerank-bg);
  color: var(--tag-rerank-text);
}
</style>
