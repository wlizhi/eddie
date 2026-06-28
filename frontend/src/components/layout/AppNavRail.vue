<script setup lang="ts">
import {computed, ref} from 'vue'
import {Bot, MessageSquare, Moon, Paintbrush, Settings, Sun} from '@lucide/vue'
import {
  applyDisplay,
  displaySettings,
  findTheme,
  getThemes,
  saveDisplaySettings,
} from '@/composables/useDisplaySettings'
import {useIconSize} from '@/composables/useIconSize'

const {iconSizeLg} = useIconSize()

defineProps<{
  activeNav: string
}>()

const emit = defineEmits<{
  navigate: [{ key: string; route: string }]
}>()

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

/** ===== 移动端底部导航栏切换 ===== */
const navExpanded = ref(true)

function toggleNav() {
  navExpanded.value = !navExpanded.value
}

function onNavItemClick(key: string) {
  const allItems = [...mainNavItems, ...bottomNavItems]
  const target = allItems.find((n) => n.key === key)
  if (!target) return
  emit('navigate', {key: target.key, route: target.route})
}
</script>

<template>
  <nav class="nav-rail" :class="{ 'nav-expanded': navExpanded }">
    <!-- 移动端底部导航手柄（位于导航栏顶部，点击切换展开/收起） -->
    <div
        class="mobile-nav-handle"
        @click.stop="toggleNav"
    >
      <div class="nav-handle-bar"/>
    </div>

    <div class="nav-items-row">
      <div class="nav-rail-top">
        <button
            v-for="item in mainNavItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeNav === item.key }"
            :title="item.label"
            @click="onNavItemClick(item.key)"
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
            @click="onNavItemClick(item.key)"
        >
          <component :is="item.icon" :size="iconSizeLg" :stroke-width="1.8"/>
          <span class="nav-tooltip">{{ item.label }}</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<style scoped>
/* ===== Nav Rail ===== */
.nav-rail {
  position: fixed;
  left: 0;
  top: 0;
  width: 3rem;
  min-width: 3rem;
  height: 100vh;
  height: 100dvh;
  background: var(--bg-nav-rail);
  border-right: 1px solid var(--border-default);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-4) 0;
  z-index: 100;
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

/* 桌面端：nav-items-row 透明传递布局 */
.nav-items-row {
  display: contents;
}

/* 桌面端隐藏移动端手柄 */
.mobile-nav-handle {
  display: none;
}


/* ===== 移动端响应式：< 768px ===== */
@media (max-width: 48rem) {
  /* ===== 移动端导航栏（固定在底部） ===== */
  .nav-rail {
    /* 必须覆盖 desktop 的 top:0 / left:0 / height:100vh */
    top: auto !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0;
    width: 100%;
    height: auto !important;
    min-width: 0;
    max-height: 3.75rem; /* 硬性限制：0.75rem手柄 + 3rem图标行 */
    overflow: hidden; /* 超出部分直接裁剪 */
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    padding: 0;
    gap: 0;
    border-right: none;
    border-top: 1px solid var(--border-default);
    z-index: 200;
    background: var(--bg-nav-rail);
    transform: translateY(0); /* 默认展开 */
    transition: transform 0.25s ease;
  }

  .nav-rail:not(.nav-expanded) {
    transform: translateY(3rem); /* 收起：图标行(3rem)下移，仅手柄(0.75rem)可见 */
  }

  /* 手柄（导航栏顶部，始终可见） */
  .mobile-nav-handle {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 0.75rem;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
    user-select: none;
    background: var(--bg-nav-rail);
    flex-shrink: 0;
  }

  .nav-handle-bar {
    width: 2rem;
    height: .25rem;
    border-radius: 2px;
    background: var(--text-tertiary);
    opacity: 0.4;
    transition: opacity 0.2s, transform 0.2s;
  }

  .mobile-nav-handle:active .nav-handle-bar {
    opacity: 0.7;
    transform: scaleY(1.3);
  }

  /* 图标行容器 */
  .nav-items-row {
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 3rem;
    padding: 0 var(--space-2);
  }

  .nav-rail-top,
  .nav-rail-bottom {
    flex-direction: row;
    gap: var(--space-1);
    flex: none;
    padding: 0;
  }

  .nav-rail-top {
    justify-content: flex-start;
  }

  .nav-rail-bottom {
    justify-content: flex-end;
    margin-left: auto;
  }

  .nav-rail-divider {
    display: none; /* 隐藏分割线 */
  }

  .nav-item {
    width: var(--touch-btn-size, 2.25rem);
    height: var(--touch-btn-size, 2.25rem);
    border-radius: 6px;
  }

  .nav-tooltip {
    display: none !important;
  }
}
</style>
