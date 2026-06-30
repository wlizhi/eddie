<!--
 * @author Eddie
 * @date 2026-06-30
 *
 * macOS 标题栏（hiddenInset 模式）
 * - 背景使用 --bg-primary，与页面完美融合
 * - -webkit-app-region: drag → 操作系统原生处理双击最大化，零卡顿
 * - 无边框/无分隔线，消除边界感
 -->

<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'

const isElectron = ref(false)
const isMaximized = ref(false)

const api = (window as any).electronAPI
if (api) {
  isElectron.value = true
  // 确保 CSS 变量在首次渲染前设置（index.html 已有，这里冗余保证）
  document.documentElement.style.setProperty('--title-bar-height', '38px')
}

onMounted(() => {
  if (!api) return
  api.isMaximized().then((m: boolean) => {
    isMaximized.value = m
  })
  api.onMaximizedChange((m: boolean) => {
    isMaximized.value = m
  })
})

onBeforeUnmount(() => {
  if (api) api.removeMaximizedChangeListener()
})

function onMinimize() {
  api?.minimize()
}

function onMaximize() {
  api?.maximize()
}

function onClose() {
  api?.close()
}
</script>

<template>
  <div v-if="isElectron" class="title-bar">
    <!-- 左侧留空 → macOS 交通灯浮层，设 no-drag 避免拦截点击 -->
    <div class="title-bar-spacer"/>

    <!-- 拖拽区域：-webkit-app-region drag → OS 原生处理双击最大化，无卡顿 -->
    <div class="title-bar-drag"/>

    <!-- 窗口控制按钮（备选） -->
    <div class="title-bar-controls">
      <button class="ctl-btn ctl-close" @click="onClose" title="关闭">
        <svg width="10" height="10" viewBox="0 0 10 10">
          <path d="M1 1l8 8M9 1l-8 8" stroke="currentColor" stroke-width="1.2" fill="none"/>
        </svg>
      </button>
      <button class="ctl-btn ctl-minimize" @click="onMinimize" title="最小化">
        <svg width="10" height="10" viewBox="0 0 10 10">
          <path d="M2 5h6" stroke="currentColor" stroke-width="1.2" fill="none"/>
        </svg>
      </button>
      <button class="ctl-btn ctl-maximize" @click="onMaximize" :title="isMaximized ? '还原' : '最大化'">
        <svg v-if="isMaximized" width="10" height="10" viewBox="0 0 10 10">
          <rect x="2.5" y="0.5" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1" fill="none"/>
          <path d="M2.5 3H1.5a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1V7.5" stroke="currentColor" stroke-width="1"
                fill="none"/>
        </svg>
        <svg v-else width="10" height="10" viewBox="0 0 10 10">
          <rect x="1" y="1" width="8" height="8" rx="1" stroke="currentColor" stroke-width="1" fill="none"/>
        </svg>
      </button>
    </div>
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

.title-bar-controls {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-right: 12px;
  height: 100%;
  -webkit-app-region: no-drag;
}

.ctl-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background 0.15s, color 0.15s;
}

.ctl-btn:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.ctl-close:hover {
  background: rgba(255, 59, 48, 0.15);
  color: #ff3b30;
}
</style>
