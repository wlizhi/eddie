<!--
  EmptyState.vue — 空状态引导页

  功能：
  - 当没有消息时显示欢迎界面
  - 提供快捷建议问题，点击后填入输入框

  与父组件通信：
  - selectSuggestion (string) — 用户点击了某条建议问题
-->
<script setup lang="ts">
const emit = defineEmits<{
  selectSuggestion: [text: string]
}>()

const suggestions = [
  '帮我写一段 Python 代码',
  '解释一下什么是 RESTful API',
  'Vue 3 和 React 有什么区别',
  '翻译一段英文文本',
]
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon">💬</div>
    <h2>开始新对话</h2>
    <p class="empty-hint">选择模型，输入你的问题，开始与 AI 助手交流</p>
    <div class="suggestions">
      <div
          v-for="s in suggestions"
          :key="s"
          class="suggestion-card"
          @click="emit('selectSuggestion', s)"
      >
        {{ s }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
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

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 24px;
  max-width: 400px;
}

.suggestion-card {
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  text-align: center;
  transition: background 0.15s;
}

.suggestion-card:hover {
  background: #f5f5f5;
}
</style>
