<!--
 * @author Eddie
 * {@code @date} 2026-07-26
-->

<template>
  <div class="panel">
    <div class="settings-group">
      <div class="group-label">
        <Info :size="16" :stroke-width="2" class="group-icon"/>
        关于 Eddie
      </div>

      <!-- Logo + 应用名 -->
      <div class="about-header">
        <div class="app-logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" :fill="accentColor"/>
            <text x="24" y="30" text-anchor="middle" fill="white"
                  font-size="22" font-weight="700" font-family="system-ui">E
            </text>
          </svg>
        </div>
        <div class="app-info">
          <span class="app-name">Eddie</span>
          <span class="app-desc">个人 AI 助手</span>
        </div>
      </div>

      <!-- 版本号 -->
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">当前版本</span>
          <span class="setting-hint">{{ versionText }}</span>
        </div>
      </div>

      <!-- 检查更新 -->
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">检查更新</span>
          <span class="setting-hint">从 GitHub 检查新版本</span>
        </div>
        <n-button
            :loading="checking"
            :disabled="checking"
            size="small"
            @click="handleCheckUpdate"
        >
          检查更新
        </n-button>
      </div>

      <!-- GitHub 仓库 -->
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">源代码</span>
          <span class="setting-hint">GitHub 仓库</span>
        </div>
        <n-button
            tag="a"
            :href="GITHUB_REPO"
            target="_blank"
            rel="noopener noreferrer"
            text
            class="github-btn"
            @click.prevent="openExternal(GITHUB_REPO)"
        >
          <template #icon>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
            </svg>
          </template>
          {{ GITHUB_REPO_TEXT }}
        </n-button>
      </div>

      <!-- 问题反馈 -->
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">问题反馈</span>
          <span class="setting-hint">在 GitHub 提交 Issue</span>
        </div>
        <n-button
            tag="a"
            :href="GITHUB_ISSUES"
            target="_blank"
            rel="noopener noreferrer"
            text
            class="feedback-btn"
            @click.prevent="openExternal(GITHUB_ISSUES)"
        >
          <template #icon>
            <Bug :size="16" :stroke-width="2"/>
          </template>
          反馈
        </n-button>
      </div>

    </div>
  </div>

  <!-- 更新结果弹窗 -->
  <n-modal
      v-model:show="showModal"
      preset="card"
      :title="modalTitle"
      style="max-width: 420px; width: 90%;"
      :mask-closable="false"
  >
    <div class="update-modal-body">
      <template v-if="updateResult?.hasUpdate">
        <div class="update-found">
          <span class="update-label">当前版本：</span>
          <span class="update-version">{{ updateResult.currentVersion }}</span>
        </div>
        <div class="update-found">
          <span class="update-label">最新版本：</span>
          <span class="update-version latest">{{ updateResult.latestVersion }}</span>
        </div>
        <div v-if="updateResult.releaseNotes" class="release-notes">
          <div class="release-notes-title">更新内容</div>
          <div class="release-notes-body">{{ updateResult.releaseNotes }}</div>
        </div>
        <div class="update-actions">
          <n-button
              tag="a"
              type="primary"
              :href="updateResult.downloadUrl"
              target="_blank"
              rel="noopener noreferrer"
              @click.prevent="openExternal(updateResult.downloadUrl); showModal = false"
          >
            去下载
          </n-button>
          <n-button @click="showModal = false">
            稍后再说
          </n-button>
        </div>
      </template>

      <template v-else>
        <div class="update-latest">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
               :stroke="accentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <span>{{ updateResult?.message || '已是最新版本' }}</span>
        </div>
        <div class="update-actions" style="justify-content: center;">
          <n-button @click="showModal = false">
            知道了
          </n-button>
        </div>
      </template>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {NButton, NModal} from 'naive-ui'
import {Bug, Info} from '@lucide/vue'
import {checkUpdate, fetchAppVersion, type UpdateCheckResult} from '@/api/app'
import {showToast} from '@/composables/useToast'

const GITHUB_REPO = 'https://github.com/wlizhi/eddie'
const GITHUB_REPO_TEXT = 'wlizhi/eddie'
const GITHUB_ISSUES = 'https://github.com/wlizhi/eddie/issues'

const versionText = ref('加载中...')
const checking = ref(false)
const showModal = ref(false)
const updateResult = ref<UpdateCheckResult | null>(null)
const modalTitle = ref('')

/**
 * 获取强调色 CSS 变量，用于 logo 图标颜色
 */
const accentColor = computed(() => {
  if (typeof document === 'undefined') return 'var(--accent-color)'
  return getComputedStyle(document.documentElement).getPropertyValue('--accent-color').trim() || '#6366f1'
})

/** 打开外部链接（Electron 环境用 shell.openExternal，浏览器直接打开） */
function openExternal(url: string) {
  // 检查是否在 Electron 环境
  if (typeof window !== 'undefined' && (window as any).electronAPI?.openExternal) {
    (window as any).electronAPI.openExternal(url)
  } else {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

/** 检查更新 */
async function handleCheckUpdate() {
  checking.value = true
  try {
    const result = await checkUpdate()
    updateResult.value = result
    if (result.hasUpdate) {
      modalTitle.value = '发现新版本'
    } else {
      modalTitle.value = '版本检查'
    }
    showModal.value = true
  } catch (err: any) {
    showToast('检查更新失败: ' + (err.message || '网络异常'), 'error')
  } finally {
    checking.value = false
  }
}

onMounted(async () => {
  try {
    const info = await fetchAppVersion()
    versionText.value = info.currentVersion
  } catch {
    versionText.value = '未知'
  }
})
</script>

<style scoped>
.panel {
  max-width: 30rem;
}

.settings-group {
  margin-bottom: 24px;
}

.group-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-lighter);
}

.group-icon {
  color: var(--text-tertiary);
}

.about-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0 16px;
}

.app-logo {
  flex-shrink: 0;
}

.app-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.app-desc {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.setting-info {
  flex: 1;
  min-width: 0;
}

.setting-label {
  display: block;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.setting-hint {
  display: block;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.4;
}

.github-btn,
.feedback-btn {
  color: var(--text-secondary);
  transition: color 0.2s;
}

.github-btn:hover,
.feedback-btn:hover {
  color: var(--accent-color);
}

/* ===== 更新弹窗 ===== */
.update-modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.update-found {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.update-label {
  color: var(--text-tertiary);
  min-width: 80px;
}

.update-version {
  font-weight: 600;
}

.update-version.latest {
  color: var(--accent-color);
}

.release-notes {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.release-notes-title {
  font-size: var(--font-size-small);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.release-notes-body {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.update-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.update-latest {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 0;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}
</style>
