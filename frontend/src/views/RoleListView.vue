<script setup lang="ts">
// RoleListView — 角色列表页面
// TODO: 展示助手/智能体角色列表，支持增删改
</script>

<template>
  <div class="role-list-view">
    <div class="role-empty">
      <div class="empty-icon">🎭</div>
      <h2>角色管理</h2>
      <p class="empty-hint">自定义助手和智能体的角色，设置 System Prompt</p>
    </div>
  </div>
</template>

<style scoped>
.role-list-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.role-empty {
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
  color: var(--text-tertiary);
  font-size: var(--font-size-base);
}
</style>
