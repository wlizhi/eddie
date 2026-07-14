<!--
 * @author Eddie
 * @date 2026-06-30
 *
 * 自定义标题栏（仅 macOS hiddenInset 模式需要）
 * - macOS: hiddenInset 移除原生标题栏但保留交通灯，此组件提供拖拽区域
 * - Windows/Linux: 使用原生标题栏或 hidden + overlay，无需此组件
 * - 背景使用 --bg-primary，与页面完美融合
 * - -webkit-app-region: drag → 操作系统原生处理双击最大化，零卡顿
 * - 无边框/无分隔线，消除边界感
 -->

<script setup lang="ts">
import {ref, onMounted, onUnmounted} from 'vue'

const isMacTitleBar = ref(false)
const isFullscreen = ref(false)

onMounted(() => {
  // 仅在 macOS Electron 环境下显示（titleBarStyle: 'hiddenInset'）
  const api = (window as any).electronAPI
  if (api && navigator.platform?.toLowerCase().includes('mac')) {
    isMacTitleBar.value = true
    document.documentElement.style.setProperty('--title-bar-height', '38px')

    // 监听 macOS 原生全屏状态变化 → 全屏时隐藏 TitleBar，让内容铺满
    api.onFullscreenChange((fullscreen: boolean) => {
      isFullscreen.value = fullscreen
      document.documentElement.style.setProperty(
        '--title-bar-height',
        fullscreen ? '0px' : '38px'
      )
    })
  }
})

onUnmounted(() => {
  const api = (window as any).electronAPI
  if (api) {
    api.removeFullscreenChangeListener()
  }
})
</script>

<template>
  <div v-if="isMacTitleBar && !isFullscreen" class="title-bar">
    <!-- 左侧留空 → macOS 交通灯浮层，设 no-drag 避免拦截点击 -->
    <div class="title-bar-spacer"/>

    <!-- 拖拽区域：-webkit-app-region drag → OS 原生处理双击最大化，无卡顿 -->
    <div class="title-bar-drag"/>
  </div>
</template>

<style scoped>
.title-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 38px;
  z-index: 9999;
  display: flex;
  align-items: center;
  background: var(--bg-primary); /* 跟随主题，与页面融合 */
  /* 无 border / box-shadow / 分隔线 */
  user-select: none;
}

/* 交通灯留空区，不拦截点击 */
.title-bar-spacer {
  width: 80px;
  min-width: 80px;
  height: 100%;
  flex-shrink: 0;
  -webkit-app-region: no-drag;
}

/* ⭐ drag 区域 → macOS 原生处理双击最大化，零卡顿 */
.title-bar-drag {
  flex: 1;
  height: 100%;
  -webkit-app-region: drag;
}

</style>
