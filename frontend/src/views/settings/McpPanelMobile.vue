<!--
 * @author Eddie
 * @date 2026-06-28
-->

<!--
  McpPanelMobile.vue — 移动端 MCP 服务管理（精简版）

  功能：
  - 内置 MCP 服务和用户自定义服务统一列表展示
  - 每个服务显示名称、类型标签、工具数量
  - 点击卡片可展开查看工具列表
  - 启用/禁用切换
  - 无新增/编辑/删除功能（桌面端做）
-->
<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {ChevronDown, Network} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import {listMcpServers, updateMcpStatus} from '@/api/mcpServer'
import type {McpServer} from '@/types/mcpServer'
import {TRANSPORT_CLASSES, TRANSPORT_LABELS} from '@/types/mcpServer'
import {showToast} from '@/composables/useToast'

const {iconSizeSm} = useIconSize()

const loading = ref(false)
const servers = ref<McpServer[]>([])
const expandedId = ref<number | null>(null)

/** 已排序：内置工具（BUILT_IN）在前，自定义（USER）次之，第三方（PROVIDER）最后 */
const sortedServers = computed(() => {
  const list = [...servers.value]
  list.sort((a, b) => {
    const order = {BUILT_IN: 0, USER: 1, PROVIDER: 2}
    return (order[a.sourceType] ?? 1) - (order[b.sourceType] ?? 1)
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

/** 展开/收起服务卡片 */
function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
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
          class="mcp-card-mobile"
          :class="{ expanded: expandedId === server.id }"
      >
        <div class="mcp-card-header" @click="toggleExpand(server.id)">
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
              <span v-if="server.sourceType === 'BUILT_IN'" class="mcp-tag-mobile built-in">内置</span>
              <span v-else-if="server.sourceType === 'PROVIDER'" class="mcp-tag-mobile built-in">第三方</span>
              <span v-if="server.tools?.length" style="font-size:var(--font-size-xs);color:var(--text-tertiary)">
                {{ server.tools.length }} 工具
              </span>
            </div>
          </div>
          <ChevronDown :size="16" :stroke-width="2"
                       class="mcp-chevron"
                       :class="{ expanded: expandedId === server.id }"/>
          <label style="flex-shrink:0;display:flex;align-items:center;cursor:pointer"
                 @click.stop="toggleServer(server)">
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

        <!-- 展开的工具列表 -->
        <div v-if="expandedId === server.id" class="mcp-detail-mobile">
          <div v-if="server.tools?.length" class="mcp-tool-list-mobile">
            <div v-for="tool in server.tools" :key="tool.id" class="mcp-tool-item-mobile">
              <div class="mcp-tool-name-mobile">{{ tool.name }}</div>
              <div v-if="tool.description" class="mcp-tool-desc-mobile">{{ tool.description }}</div>
            </div>
          </div>
          <div v-else class="mcp-tool-empty-mobile">暂无工具</div>
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
.mcp-card-mobile {
  border: 1px solid var(--border-default);
  border-radius: 12px;
  padding: var(--space-4);
  background: var(--bg-secondary);
  transition: border-color 0.15s;
}

.mcp-card-mobile:active {
  border-color: var(--accent-light-border);
}

.mcp-card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mcp-chevron {
  flex-shrink: 0;
  color: var(--text-tertiary);
  transition: transform 0.2s;
}

.mcp-chevron.expanded {
  transform: rotate(180deg);
}

/* 展开详情 */
.mcp-detail-mobile {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-lighter);
}

.mcp-tool-list-mobile {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.mcp-tool-item-mobile {
  padding: var(--space-3) var(--space-4);
  background: var(--bg-primary);
  border: 1px solid var(--border-lighter);
  border-radius: 8px;
}

.mcp-tool-name-mobile {
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-primary);
  font-family: Monaco, 'Fira Code', monospace;
}

.mcp-tool-desc-mobile {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-1);
  line-height: 1.4;
}

.mcp-tool-empty-mobile {
  padding: var(--space-4) 0;
  text-align: center;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}

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
