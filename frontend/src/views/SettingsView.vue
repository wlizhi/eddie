<!--
 * @author Eddie
 * @date 2026-06-20
-->

<script setup lang="ts">
import {computed, ref} from 'vue'
import {useMobile} from '@/composables/useMobile'
import {Clock, Cpu, Globe, Monitor, MousePointerClick, Network, Puzzle, Radio, Settings, Zap} from '@lucide/vue'
import ModelProviderPanel from './settings/ModelProviderPanel.vue'
import DefaultModelPanel from './settings/DefaultModelPanel.vue'
import GeneralPanel from './settings/GeneralPanel.vue'
import DisplayPanel from './settings/DisplayPanel.vue'
import McpPanel from './settings/McpPanel.vue'
import SkillsPanel from './settings/SkillsPanel.vue'
import WebSearchPanel from './settings/WebSearchPanel.vue'
import ChannelsPanel from './settings/ChannelsPanel.vue'
import ScheduledTasksPanel from './settings/ScheduledTasksPanel.vue'
import SelectionAssistantPanel from './settings/SelectionAssistantPanel.vue'
import SettingsViewMobile from './settings/SettingsViewMobile.vue'

const {isMobile} = useMobile()

interface NavSection {
  key: string
  label: string
  icon: any
}

interface NavGroup {
  items: NavSection[]
}

const navGroups: NavGroup[] = [
  {
    items: [
      {key: 'model-provider', label: '模型服务', icon: Cpu},
      {key: 'default-model', label: '默认模型', icon: Zap},
    ],
  },
  {
    items: [
      {key: 'general', label: '常规设置', icon: Settings},
      {key: 'display', label: '显示设置', icon: Monitor},
      {key: 'selection-assistant', label: '划词助手', icon: MousePointerClick},
    ],
  },
  {
    items: [
      {key: 'mcp', label: 'MCP 服务', icon: Network},
      {key: 'skills', label: '技能', icon: Puzzle},
      {key: 'web-search', label: '网络搜索', icon: Globe},
      {key: 'channels', label: '频道', icon: Radio},
      {key: 'scheduled-tasks', label: '定时任务', icon: Clock},
    ],
  },
]

const panelMap: Record<string, any> = {
  'model-provider': ModelProviderPanel,
  'default-model': DefaultModelPanel,
  'general': GeneralPanel,
  'display': DisplayPanel,
  'selection-assistant': SelectionAssistantPanel,
  'mcp': McpPanel,
  'skills': SkillsPanel,
  'web-search': WebSearchPanel,
  'channels': ChannelsPanel,
  'scheduled-tasks': ScheduledTasksPanel,
}

const activeKey = ref<string>('model-provider')
const currentPanel = computed(() => panelMap[activeKey.value])
</script>

<template>
  <!-- 移动端：独立 mobile 组件 -->
  <SettingsViewMobile v-if="isMobile"/>

  <!-- 桌面端：保持现有布局不变 -->
  <template v-else>
    <div class="settings-layout">
      <nav class="settings-nav">
        <template v-for="(group, gi) in navGroups" :key="gi">
          <button
              v-for="item in group.items"
              :key="item.key"
              class="nav-btn"
              :class="{ active: activeKey === item.key }"
              @click="activeKey = item.key"
          >
            <component :is="item.icon" :size="16" :stroke-width="1.8" class="nav-btn-icon"/>
            <span>{{ item.label }}</span>
          </button>
          <div v-if="gi < navGroups.length - 1" class="nav-divider"/>
        </template>
      </nav>
      <div class="settings-content">
        <component :is="currentPanel"/>
      </div>
    </div>
  </template>
</template>

<style src="./settings/settings.css" scoped/>
