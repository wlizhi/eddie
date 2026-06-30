<!--
  EmptyState.vue — 空状态引导页

  功能：
  - 当没有消息时显示欢迎界面
  - 提供快捷建议问题，点击后填入输入框
  - 每次刷新从内置问题池中随机抽取 4 条展示

  与父组件通信：
  - selectSuggestion (string) — 用户点击了某条建议问题

  作者：Eddie
  日期：2026-06-30
-->
<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {MessageSquare} from '@lucide/vue'
import {useIconSize} from '@/composables/useIconSize'

const {iconSizeXxl} = useIconSize()

const emit = defineEmits<{
  selectSuggestion: [text: string]
}>()

/** 内置问题池 */
const questionPool = [
  '帮我写一段 Python 代码',
  '解释一下什么是 RESTful API',
  'Vue 3 和 React 有什么区别',
  '翻译一段英文文本',
  '用通俗的语言解释量子计算',
  '帮我写一封正式的商务邮件',
  '推荐几本关于系统设计的书',
  '如何优化 SQL 查询性能',
  '给我一份前端学习路线图',
  '简述 Docker 和 Kubernetes 的区别',
  '帮我总结这篇文章的要点',
  '如何提高代码的可读性',
] as const

/** 当前展示的建议问题（每次刷新随机选 4 条） */
const suggestions = ref<string[]>([])

onMounted(() => {
  // Fisher-Yates 洗牌后取前 4 条
  const shuffled = [...questionPool]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  suggestions.value = shuffled.slice(0, 4)
})
</script>

<template>
  <div class="empty-state">
    <MessageSquare
        :size="iconSizeXxl"
        :stroke-width="1.5"
        class="empty-icon"
    />
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
  padding: var(--space-20);
  gap: var(--space-6);
}

.empty-icon {
  color: var(--text-tertiary);
  margin-bottom: var(--space-4);
}

h2 {
  font-size: var(--font-size-title);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.empty-hint {
  color: var(--text-tertiary);
  font-size: var(--font-size-base);
  margin: 0;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-12);
  max-width: 28rem;
  width: 100%;
}

.suggestion-card {
  padding: var(--space-6) var(--space-8);
  border: 1px solid var(--border-light);
  border-radius: var(--space-4);
  cursor: pointer;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  text-align: center;
  transition: background 0.15s, border-color 0.15s;
  line-height: 1.5;
}

.suggestion-card:hover {
  background: var(--bg-hover);
  border-color: var(--border-hover);
}
</style>
