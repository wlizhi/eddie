<template>
  <aside class="mcp-sidebar">
    <button
        v-for="tab in tabs"
        :key="tab.key"
        class="mcp-sidebar-tab"
        :class="{ active: activeTab === tab.key }"
        @click="$emit('update:activeTab', tab.key)"
    >
      <component :is="tab.icon" :size="16" :stroke-width="1.8" class="mcp-sidebar-tab-icon"/>
      <span>{{ tab.label }}</span>
    </button>
  </aside>
</template>

<script setup lang="ts">
import {Compass, HardDrive, Network} from '@lucide/vue'

export type McpSidebarTab = 'builtin' | 'mcp' | 'discover'

defineProps<{
  activeTab: McpSidebarTab
}>()

defineEmits<{
  'update:activeTab': [tab: McpSidebarTab]
}>()

const tabs: { key: McpSidebarTab; label: string; icon: any }[] = [
  {key: 'builtin', label: '内置工具', icon: HardDrive},
  {key: 'mcp', label: 'MCP 扩展', icon: Network},
  {key: 'discover', label: '发现', icon: Compass},
]
</script>

<style scoped src="./mcp-server.css"></style>
