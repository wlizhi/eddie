<!--
 * @author Eddie
 * @date 2026-06-20
-->

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {darkTheme, NConfigProvider, NDialogProvider} from 'naive-ui'
import {displaySettings, loadDisplaySettings} from '@/composables/useDisplaySettings'
import {naiveThemeOverrides} from '@/composables/useNaiveThemeOverrides'
import AppNavRail from '@/components/layout/AppNavRail.vue'
import AppContextPanel from '@/components/layout/AppContextPanel.vue'
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

const isMobile = window.matchMedia('(max-width: 48rem)').matches
/** 面板折叠状态（chat/agent/settings 各页面独立记忆） */
const panelCollapsed = ref<Record<string, boolean>>({
  chat: isMobile,
  agent: true,
  settings: true,
})

const panelKeys = ['chat', 'agent', 'settings']

/** 根据当前路由路径确定活跃导航项 */
const activeNav = computed(() => {
  const path = route.path
  if (path.startsWith('/chat')) return 'chat'
  if (path.startsWith('/agent')) return 'agent'
  if (path.startsWith('/settings')) return 'settings'
  return 'chat'
})

const isPanelVisible = computed(() => !panelCollapsed.value[activeNav.value])

/** 导航栏点击：路由跳转 + 面板折叠状态切换 */
function onNavNavigate({key, route: navRoute}: { key: string; route: string }) {
  router.push(navRoute)
  const newState: Record<string, boolean> = {}
  for (const k of panelKeys) {
    newState[k] = k !== key
  }
  if (activeNav.value === key) {
    newState[key] = !panelCollapsed.value[key]
  }
  // 设置页面不需要侧栏面板，始终折叠
  newState['settings'] = true
  panelCollapsed.value = newState
}

/** 手动切换侧栏面板折叠/展开 */
function toggleCollapse() {
  const key = activeNav.value
  panelCollapsed.value = {
    ...panelCollapsed.value,
    [key]: !panelCollapsed.value[key],
  }
}
</script>

<template>
  <!-- 全局背景层：承载主题渐变装饰，位于所有页面内容之下 -->
  <div class="app-backdrop"/>
  <NConfigProvider :theme="naiveTheme" :theme-overrides="naiveThemeOverrides">
    <NDialogProvider>
      <AppNavRail
          :active-nav="activeNav"
          @navigate="onNavNavigate"
      />

      <div class="app-layout">
        <AppContextPanel
            :visible="isPanelVisible"
            @toggle="toggleCollapse"
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

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  height: 100dvh;
  width: 100vw;
  position: relative;
  z-index: 1;
  padding-left: 3rem;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 移动端响应式：< 768px ===== */
@media (max-width: 48rem) {
  .app-layout {
    flex-direction: column;
    padding-left: 0;
    padding-bottom: 0.75rem;
    transition: padding-bottom 0.25s ease;
  }

  .main-content {
    padding-bottom: 0;
  }
}
</style>
