<script setup lang="ts">
// AgentView — 智能体任务执行页面
// TODO: 智能体任务创建、步骤执行、结果展示
</script>

<template>
  <div class="agent-view">
    <div class="agent-empty">
      <div class="empty-icon">🤖</div>
      <h2>智能体</h2>
      <p class="empty-hint">创建任务，让 AI 智能体自动规划和执行</p>
    </div>
  </div>
</template>

<style scoped>
.agent-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.agent-empty {
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
