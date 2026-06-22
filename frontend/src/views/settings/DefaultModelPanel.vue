<template>
  <div class="panel">
    <h3 class="panel-title">默认模型</h3>
    <p class="panel-desc">配置三类预设模型，供助手、智能体、翻译等功能默认选用。</p>

    <div class="config-card">
      <div class="config-header">
        <Zap :size="18" :stroke-width="2"/>
        <span>默认模型</span>
      </div>
      <p class="config-hint">创建助手或智能体时，未指定模型则使用此模型。建议选择综合能力强的模型。</p>
      <div class="config-row">
        <span class="config-label">服务商</span>
        <span class="config-value placeholder">DeepSeek</span>
      </div>
      <div class="config-row">
        <span class="config-label">模型</span>
        <span class="config-value placeholder">deepseek-v4-pro</span>
      </div>
    </div>

    <div class="config-card">
      <div class="config-header">
        <Zap :size="18" :stroke-width="2" class="fast-icon"/>
        <span>快速模型</span>
      </div>
      <p class="config-hint">用于生成会话标题、中期记忆压缩、长期记忆摘要等轻量杂活。建议选择便宜快速的模型。</p>
      <div class="config-row">
        <span class="config-label">服务商</span>
        <span class="config-value placeholder">DeepSeek</span>
      </div>
      <div class="config-row">
        <span class="config-label">模型</span>
        <span class="config-value placeholder">deepseek-v4-flash</span>
      </div>
    </div>

    <div class="config-card">
      <div class="config-header">
        <Globe :size="18" :stroke-width="2" class="translate-icon"/>
        <span>翻译模型</span>
      </div>
      <p class="config-hint">翻译功能专用模型。可根据需要选择翻译能力强的模型。</p>
      <div class="config-row">
        <span class="config-label">服务商</span>
        <span class="config-value placeholder">未配置</span>
      </div>
      <div class="config-row">
        <span class="config-label">模型</span>
        <span class="config-value placeholder">—</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {Globe, Zap} from '@lucide/vue'
</script>

<style scoped>
.panel {
  max-width: 640px;
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

.config-card {
  border: 1px solid #e6e8ec;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: #fafbfc;
}

.config-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
}

.config-header .fast-icon {
  color: #10b981;
}

.config-header .translate-icon {
  color: #8b5cf6;
}

.config-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 14px;
  line-height: 1.5;
}

.config-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}

.config-label {
  width: 56px;
  font-size: 13px;
  color: #6b7280;
  flex-shrink: 0;
}

.config-value {
  font-size: 13px;
  color: #1f1f1f;
}

.config-value.placeholder {
  color: #9ca3af;
  font-style: italic;
}
</style>
