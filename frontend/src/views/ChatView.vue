<script setup lang="ts">
// ChatView — 聊天主页面
// TODO: 实现消息流、输入框、模型选择器、思考等级
</script>

<template>
  <div class="chat-view">
    <div class="chat-empty">
      <div class="empty-icon">💬</div>
      <h2>开始新对话</h2>
      <p class="empty-hint">选择模型，输入你的问题，开始与 AI 助手交流</p>
      <div class="suggestions">
        <div class="suggestion-card">帮我写一段 Python 代码</div>
        <div class="suggestion-card">解释一下什么是 RESTful API</div>
        <div class="suggestion-card">帮我优化这段代码</div>
        <div class="suggestion-card">翻译一段英文文本</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-empty {
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
