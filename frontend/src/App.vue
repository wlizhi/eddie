<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {darkTheme, NConfigProvider} from 'naive-ui'
import {Bot, ChevronLeft, ChevronRight, MessageSquare, Settings} from '@lucide/vue'
import {displaySettings, loadDisplaySettings} from '@/composables/useDisplaySettings'
import ToastNotification from '@/components/common/ToastNotification.vue'

onMounted(() => {
  loadDisplaySettings()
})

/** Naive UI 主题：跟随 displaySettings 响应式变化 */
const naiveTheme = computed(() =>
    displaySettings.themeMode === 'dark' ? darkTheme : null
)

const router = useRouter()
const route = useRoute()

type NavItem = {
  key: string
  icon: typeof MessageSquare
  label: string
  route: string
}

const mainNavItems: NavItem[] = [
  {key: 'chat', icon: MessageSquare, label: '助手', route: '/chat'},
  {key: 'agent', icon: Bot, label: '智能体', route: '/agent'},
]

const bottomNavItems: NavItem[] = [
  {key: 'settings', icon: Settings, label: '设置', route: '/settings'},
]

const allKeys = [...mainNavItems, ...bottomNavItems].map((n) => n.key)

const activeNav = computed(() => {
  const path = route.path
  const allItems = [...mainNavItems, ...bottomNavItems]
  const item = allItems.find((n) => path.startsWith(n.route))
  return item?.key ?? 'chat'
})

const panelCollapsed = ref<Record<string, boolean>>({
  chat: false,
  agent: true,
  settings: true,
})

const isPanelVisible = computed(() => !panelCollapsed.value[activeNav.value])

function togglePanel(key: string) {
  const allItems = [...mainNavItems, ...bottomNavItems]
  const target = allItems.find((n) => n.key === key)
  if (!target) return
  router.push(target.route)

  const newState: Record<string, boolean> = {}
  for (const k of allKeys) {
    newState[k] = k !== key
  }
  if (activeNav.value === key) {
    newState[key] = !panelCollapsed.value[key]
  }
  // 设置页面不需要侧栏面板，始终折叠
  newState['settings'] = true
  panelCollapsed.value = newState
}

function toggleCollapse() {
  const key = activeNav.value
  panelCollapsed.value = {
    ...panelCollapsed.value,
    [key]: !panelCollapsed.value[key],
  }
}

</script>

<template>
  <NConfigProvider :theme="naiveTheme">
  <div class="app-layout">
    <!-- Nav Rail -->
    <nav class="nav-rail">
      <div class="nav-rail-top">
        <button
            v-for="item in mainNavItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeNav === item.key }"
            :title="item.label"
            @click="togglePanel(item.key)"
        >
          <component :is="item.icon" :size="20" :stroke-width="1.8"/>
          <span class="nav-tooltip">{{ item.label }}</span>
        </button>
      </div>

      <div class="nav-rail-divider"/>

      <div class="nav-rail-bottom">
        <button
            v-for="item in bottomNavItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeNav === item.key }"
            :title="item.label"
            @click="togglePanel(item.key)"
        >
          <component :is="item.icon" :size="20" :stroke-width="1.8"/>
          <span class="nav-tooltip">{{ item.label }}</span>
        </button>
      </div>
    </nav>

    <!-- Contextual Panel -->
    <aside class="context-panel" :class="{ collapsed: !isPanelVisible }">
      <button class="panel-collapse-btn" :title="isPanelVisible ? '折叠侧栏' : '展开侧栏'" @click="toggleCollapse">
        <component :is="isPanelVisible ? ChevronLeft : ChevronRight" :size="12" :stroke-width="2"/>
      </button>
      <div class="panel-body">
        <router-view name="panel"/>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <router-view/>
    </main>
  </div>
    <ToastNotification/>
  </NConfigProvider>
</template>

<style>
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

body {
  font-family: var(--font-family);
  font-size: var(--font-size-base);
  background: var(--bg-primary);
  color: var(--text-primary);
}

/* 确保所有表单元素继承字体设置 */
button, input, select, textarea {
  font-family: inherit;
}
</style>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
}

/* ===== Nav Rail ===== */
.nav-rail {
  width: 48px;
  min-width: 48px;
  background: var(--bg-nav-rail);
  border-right: 1px solid var(--border-default);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  z-index: 100;
  user-select: none;
}

.nav-rail-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.nav-rail-divider {
  width: 24px;
  height: 1px;
  background: var(--divider-light);
  margin: 4px 0;
  flex-shrink: 0;
}

.nav-rail-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.nav-item {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: background 0.15s, color 0.15s;
  color: var(--text-quaternary);
}

.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.nav-item.active {
  background: var(--bg-primary);
  color: var(--accent-default);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* Tooltip */
.nav-tooltip {
  display: none;
  position: absolute;
  left: 44px;
  top: 50%;
  transform: translateY(-50%);
  background: var(--bg-tooltip);
  color: var(--text-inverse);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  z-index: 200;
}

.nav-item:hover .nav-tooltip {
  display: block;
}

/* ===== Context Panel ===== */
.context-panel {
  width: 260px;
  min-width: 260px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-default);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, min-width 0.2s ease, opacity 0.15s ease;
  overflow: hidden;
  position: relative;
}

.context-panel.collapsed {
  width: 0;
  min-width: 0;
  opacity: 0;
}

.panel-collapse-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background 0.15s, color 0.15s;
  z-index: 10;
}

.panel-collapse-btn:hover {
  background: var(--bg-hover);
  color: var(--text-quaternary);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
}
</style>
