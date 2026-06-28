<template>
  <div class="panel">
    <h3 class="panel-title">常规设置</h3>
    <p class="panel-desc">应用的通用行为和偏好配置。</p>
    <div class="panel-placeholder">
      <Settings :size="48" :stroke-width="1" class="placeholder-icon"/>
      <span>常规设置功能待实现</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {Settings} from '@lucide/vue'
</script>

<style scoped>
.panel {
  max-width: 40rem;
}

.panel-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
}

.panel-desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 28px;
  line-height: 1.5;
}

.panel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #9ca3af;
  font-size: 13px;
}

.placeholder-icon {
  color: #d1d5db;
}
</style>
