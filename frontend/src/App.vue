<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {darkTheme, NConfigProvider} from 'naive-ui'
import {Bot, ChevronLeft, ChevronRight, MessageSquare, Moon, Paintbrush, Settings, Sun} from '@lucide/vue'
import {
  applyDisplay,
  displaySettings,
  findTheme,
  getThemes,
  loadDisplaySettings,
  saveDisplaySettings
} from '@/composables/useDisplaySettings'
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

/** 循环切换主题：按注册顺序轮换，亮/深色变体保持同步 */
function cycleTheme() {
  const themes = getThemes()
  const current = displaySettings.themeId
  const idx = themes.findIndex((t) => t.id === current)
  displaySettings.themeId = themes[(idx + 1) % themes.length].id
  applyDisplay()
  saveDisplaySettings().catch(() => {
  })
}

/** 切换亮/深色变体 */
function toggleThemeVariant() {
  displaySettings.themeMode = displaySettings.themeMode === 'light' ? 'dark' : 'light'
  applyDisplay()
  saveDisplaySettings().catch(() => {
  })
}

/** 当前主题信息 */
const currentTheme = computed(() => findTheme(displaySettings.themeId))

</script>

<template>
  <!-- 全局背景层：承载主题渐变装饰，位于所有页面内容之下 -->
  <div class="app-backdrop"/>
  <NConfigProvider :theme="naiveTheme" :theme-overrides="{
      Card: {
          colorModal: findTheme(displaySettings.themeId)?.variables[displaySettings.themeMode]?.['--bg-primary'] ?? '#ffffff',
      },
  }">
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
        <!-- 主题切换按钮：点击循环切换主题 -->
        <button
            class="nav-item"
            :title="'切换主题（当前：' + (currentTheme?.name ?? '默认') + '）'"
            @click="cycleTheme"
        >
          <Paintbrush :size="20" :stroke-width="1.8"/>
          <span class="nav-tooltip">{{ currentTheme?.name ?? '默认' }}</span>
        </button>
        <!-- 亮/深色切换 -->
        <button
            class="nav-item"
            :title="displaySettings.themeMode === 'dark' ? '切换浅色' : '切换深色'"
            @click="toggleThemeVariant"
        >
          <Sun v-if="displaySettings.themeMode === 'light'" :size="20" :stroke-width="1.8"/>
          <Moon v-else :size="20" :stroke-width="1.8"/>
          <span class="nav-tooltip">{{ displaySettings.themeMode === 'dark' ? '浅色' : '深色' }}</span>
        </button>
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

/* ===== 全局背景装饰层 =====
   承载主题渐变装饰，位于所有页面内容之下。
   JS 在 applyDisplaySettings() 中设置其 background 属性。 */
.app-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: var(--bg-decoration, none), var(--bg-primary);
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
  position: relative;
  z-index: 1;
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
  position: relative;
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
  /* 背景色由 body 统一提供 */
}
</style>
