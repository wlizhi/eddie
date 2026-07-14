<!--
 * @author Eddie
 * @date 2026-06-28
-->

<!--
  SettingsViewMobile.vue — 移动端设置页面

  设计要点：
  - 全屏列表式导航（类似 iOS Settings）
  - 点击条目滑入对应面板
  - 面板顶部有返回按钮回到列表
  - 独立于桌面端，不依赖 @media 查询
-->
<script setup lang="ts">
import type {Component} from 'vue'
import {computed, ref} from 'vue'
import {Cpu, Monitor, Network, Zap} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'
import ModelProviderPanelMobile from './ModelProviderPanelMobile.vue'
import DefaultModelPanel from './DefaultModelPanel.vue'
import DisplayPanel from './DisplayPanel.vue'
import McpPanelMobile from './McpPanelMobile.vue'

const {iconSizeSm} = useIconSize()

interface NavItem {
  key: string
  label: string
  icon: any
  component: Component
}

const navItems: NavItem[] = [
  {key: 'model-provider', label: '模型服务', icon: Cpu, component: ModelProviderPanelMobile},
  {key: 'default-model', label: '默认模型', icon: Zap, component: DefaultModelPanel},
  {key: 'display', label: '显示设置', icon: Monitor, component: DisplayPanel},
  {key: 'mcp', label: 'MCP 服务', icon: Network, component: McpPanelMobile},
  // 占位面板（技能/搜索/频道/定时任务）手机端暂不展示
]

/** 当前所在页：'list' 或具体 panel key */
const currentPage = ref<string>('list')

/** 当前活跃的导航项 */
const activeItem = computed<NavItem | null>(() => {
  if (currentPage.value === 'list') return null
  return navItems.find(i => i.key === currentPage.value) ?? null
})

const activeComponent = computed<Component | null>(() => activeItem.value?.component ?? null)

function navigateTo(key: string) {
  currentPage.value = key
}

function goBack() {
  currentPage.value = 'list'
}
</script>

<template>
  <div class="settings-mobile">
    <!-- 顶部导航栏 -->
    <div class="settings-mobile-header">
      <button
          v-if="currentPage !== 'list'"
          class="settings-mobile-back"
          @click="goBack"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
      </button>
      <h1 class="settings-mobile-title">
        {{ activeItem?.label ?? '设置' }}
      </h1>
    </div>

    <!-- 内容区 -->
    <div class="settings-mobile-body">
      <!-- ===== 列表导航 ===== -->
      <template v-if="currentPage === 'list'">
        <button
            v-for="item in navItems"
            :key="item.key"
            class="settings-nav-item"
            @click="navigateTo(item.key)"
        >
                    <span class="settings-nav-item-icon">
                        <component :is="item.icon" :size="iconSizeSm" :stroke-width="1.8"/>
                    </span>
          <span class="settings-nav-item-label">{{ item.label }}</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
               class="settings-nav-item-arrow">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </template>

      <!-- ===== 面板视图 ===== -->
      <Transition v-else name="slide-fade" mode="out-in">
        <div v-if="activeComponent" :key="currentPage" class="settings-panel-view">
          <component :is="activeComponent"/>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped src="./settings-mobile.css"></style>
