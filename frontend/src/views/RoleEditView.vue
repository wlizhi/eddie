<script setup lang="ts">
// RoleEditView — 角色编辑页面
// TODO: 编辑角色名称、头像、System Prompt、关联模型
</script>

<template>
  <div class="role-edit-view">
    <div class="edit-placeholder">
      <h2>编辑角色</h2>
      <p class="empty-hint">选择左侧角色列表中的角色进行编辑</p>
    </div>
  </div>
</template>

<style scoped>
.role-edit-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.edit-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
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
