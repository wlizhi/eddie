<script setup lang="ts">
// SettingsView — 全局设置页面
// TODO: 模型服务商管理、MCP 配置、显示设置
</script>

<template>
  <div class="settings-view">
    <div class="settings-placeholder">
      <div class="empty-icon">⚙️</div>
      <h2>设置</h2>
      <p class="empty-hint">管理模型服务商、MCP 连接和显示偏好</p>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.settings-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

h2 {
  font-size: 24px;
  font-weight: 600;
}

.empty-hint {
  color: #888;
  font-size: 14px;
}
</style>
