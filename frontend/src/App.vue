<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {darkTheme, NConfigProvider, NDialogProvider} from 'naive-ui'
import {Bot, ChevronLeft, ChevronRight, MessageSquare, Moon, Paintbrush, Settings, Sun} from '@lucide/vue'
import {
  applyDisplay,
  displaySettings,
  findTheme,
  getThemes,
  loadDisplaySettings,
  saveDisplaySettings
} from '@/composables/useDisplaySettings'
import {naiveThemeOverrides} from '@/composables/useNaiveThemeOverrides'
import {useIconSize} from '@/composables/useIconSize'
import ToastNotification from '@/components/common/ToastNotification.vue'

const {iconSizeLg, iconSizeXs} = useIconSize()

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
  <NConfigProvider :theme="naiveTheme" :theme-overrides="naiveThemeOverrides">
    <NDialogProvider>
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
              <component :is="item.icon" :size="iconSizeLg" :stroke-width="1.8"/>
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
              <Paintbrush :size="iconSizeLg" :stroke-width="1.8"/>
              <span class="nav-tooltip">{{ currentTheme?.name ?? '默认' }}</span>
            </button>
            <!-- 亮/深色切换 -->
            <button
                class="nav-item"
                :title="displaySettings.themeMode === 'dark' ? '切换浅色' : '切换深色'"
                @click="toggleThemeVariant"
            >
              <Sun v-if="displaySettings.themeMode === 'light'" :size="iconSizeLg" :stroke-width="1.8"/>
              <Moon v-else :size="iconSizeLg" :stroke-width="1.8"/>
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
              <component :is="item.icon" :size="iconSizeLg" :stroke-width="1.8"/>
              <span class="nav-tooltip">{{ item.label }}</span>
            </button>
          </div>
        </nav>

        <!-- Contextual Panel -->
        <aside class="context-panel" :class="{ collapsed: !isPanelVisible }">
          <button class="panel-collapse-btn" :title="isPanelVisible ? '折叠侧栏' : '展开侧栏'" @click="toggleCollapse">
            <component :is="isPanelVisible ? ChevronLeft : ChevronRight" :size="iconSizeXs" :stroke-width="2"/>
          </button>
          <div class="panel-body">
            <router-view name="panel"/>
          </div>
        </aside>

        <!-- Mobile panel backdrop (tap to close) -->
        <div
            v-if="isPanelVisible"
            class="panel-backdrop"
            @click="toggleCollapse"
        />

        <!-- Main Content -->
        <main class="main-content">
          <router-view/>
        </main>
      </div>
      <ToastNotification/>
    </NDialogProvider>
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
   JS 在 applyDisplaySettings() 中设置其 background 属性。
   backdrop-breathe 提供极微弱的透明度脉动呼吸感。 */
.app-backdrop {
  position: fixed;
  inset: -10%;
  z-index: 0;
  pointer-events: none;
  background: var(--bg-primary);
  animation: backdrop-drift 25s ease-in-out infinite;
}

/* 装饰层单独放在 ::before 上，用 drop-shadow 做呼吸，不影响外框 */
.app-backdrop::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--deco-instance, none);
  animation: backdrop-breathe 8s ease-in-out infinite;
  pointer-events: none;
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
  width: 3rem;
  min-width: 3rem;
  background: var(--bg-nav-rail);
  border-right: 1px solid var(--border-default);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-4) 0;
  z-index: 100;
  position: relative;
  user-select: none;
}

.nav-rail-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
}

.nav-rail-divider {
  width: 1.5rem;
  height: .0625rem;
  background: var(--divider-light);
  margin: var(--space-2) 0;
  flex-shrink: 0;
}

.nav-rail-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.nav-item {
  width: var(--size-btn-lg);
  height: var(--size-btn-lg);
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
  box-shadow: 0 1px 3px var(--accent-ring);
}

/* Tooltip */
.nav-tooltip {
  display: none;
  position: absolute;
  left: calc(100% + var(--space-2));
  top: 50%;
  transform: translateY(-50%);
  background: var(--bg-tooltip);
  color: var(--text-inverse);
  padding: var(--space-1) var(--space-5);
  border-radius: 6px;
  font-size: var(--font-size-xxs);
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
  width: 16.25rem;
  min-width: 16.25rem;
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
  top: var(--space-2);
  right: var(--space-2);
  width: 1.375rem;
  height: 1.375rem;
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


/* ===== 移动端响应式：< 768px ===== */
@media (max-width: 48rem) {
  .app-layout {
    flex-direction: column;
  }

  /* Nav Rail → 底部 Tab 栏 */
  .nav-rail {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    min-width: 0;
    height: 3rem;
    flex-direction: row;
    justify-content: space-around;
    padding: 0 var(--space-4);
    border-right: none;
    border-top: 1px solid var(--border-default);
    z-index: 200;
    order: 2;
  }

  .nav-rail-top,
  .nav-rail-bottom {
    flex-direction: row;
    gap: var(--space-3);
    flex: none;
  }

  .nav-rail-divider {
    width: .0625rem;
    height: 1.5rem;
    margin: 0 var(--space-2);
  }

  .nav-item {
    width: var(--touch-btn-size, 2.25rem);
    height: var(--touch-btn-size, 2.25rem);
  }

  .nav-tooltip {
    display: none !important;
  }

  /* Context Panel → 覆盖层抽屉 */
  .context-panel {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 3rem;
    width: 85vw;
    min-width: 0;
    z-index: 300;
    border-right: 1px solid var(--border-default);
    box-shadow: 2px 0 12px var(--accent-ring);
    transition: transform 0.2s ease;
    transform: translateX(0);
  }

  .context-panel.collapsed {
    width: 85vw;
    min-width: 0;
    opacity: 1;
    transform: translateX(-100%);
  }

  .panel-collapse-btn {
    display: none;
  }

  /* 遮罩层：点击关闭面板 */
  .panel-backdrop {
    display: block;
  }

  .main-content {
    padding-bottom: 3rem;
  }
}

/* 桌面端隐藏遮罩层 */
.panel-backdrop {
  display: none;
}

/* 移动端遮罩层 */
@media (max-width: 48rem) {
  .panel-backdrop {
    position: fixed;
    inset: 0;
    bottom: 3rem;
    background: var(--bg-mask);
    z-index: 250;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
  }
}

/* 暗色模式下移动端面板阴影 */
@media (max-width: 48rem) {
  .context-panel {
    box-shadow: 2px 0 12px var(--bg-mask);
  }
}
</style>
