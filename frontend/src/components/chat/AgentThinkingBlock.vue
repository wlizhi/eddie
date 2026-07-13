<!--
 * @author Eddie
 * {@code @date} 2026-07-13
 *
 * AgentThinkingBlock — 思考过程块
 *
 * 独立的 thinking 折叠/展开组件。
 *
 * 行为设计：
 * 1. 历史记录模式（页面刷新后）：
 *    - 默认折叠，不渲染任何内容
 *    - 用户点击展开 → renderMd 渲染并缓存 HTML → 再次展开直接显示缓存
 * 2. 增量模式（流式响应中）：
 *    - 收到 thinking 事件 → JS 数据增量追加，当前展开则增量渲染，折叠则不渲染
 *    - 用户思考中展开 → 渲染当前最新数据，后续增量继续追加渲染
 *    - 收到 metadata（思考完成）→ 折叠状态清除缓存（下次展开重新渲染完整内容），展开状态直接渲染最终版
 -->
<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Brain, ChevronDown} from '@lucide/vue'
import {renderMd} from '@/utils/markdown'

const props = defineProps<{
  /** 思考文本内容 */
  thinking: string
  /** 是否正在流式响应中 */
  isStreaming: boolean
  /** 是否是最新消息（用于显示"思考中..."状态） */
  isLast: boolean
  /** 当前轮次是否还有 content（有 content 表示思考完成） */
  hasContent: boolean
  /** 此轮次思考内容是否正在流式接收中（事件驱动，收到 thinking 事件为 true，收到 metadata 为 false） */
  thinkingStreaming?: boolean
}>()

/** 自身展开/折叠状态 */
const expanded = ref(false)

/** 缓存已渲染的 HTML，避免重复调用 renderMd */
const cachedHtml = ref('')

/** 切换展开/折叠 */
function toggle() {
  expanded.value = !expanded.value
}

/** 渲染并缓存 HTML */
function renderAndCache() {
  if (props.thinking) {
    cachedHtml.value = renderMd(props.thinking)
  }
}

/**
 * 展开时：有缓存直接用，无缓存则渲染当前内容
 */
watch(expanded, (val) => {
  if (val && !cachedHtml.value && props.thinking) {
    renderAndCache()
  }
})

/**
 * 流式增量数据到达且展开时 → 增量渲染
 */
watch(() => props.thinking, () => {
  if (expanded.value && props.thinking) {
    renderAndCache()
  }
})

/**
 * 思考完成（收到 metadata）：
 * - 折叠态 → 清空缓存，下次展开时重新渲染完整最终版
 * - 展开态 → 数据已通过 thinking watch 增量渲染，无需额外操作
 */
watch(() => props.thinkingStreaming, (newVal, oldVal) => {
  if (oldVal === true && newVal === false && !expanded.value) {
    cachedHtml.value = ''
  }
})

/** 是否显示"思考中..."占位 */
const showPending = computed(() =>
    !!props.thinkingStreaming
)
</script>

<template>
  <div v-if="thinking || showPending" class="thinking-section">
    <button class="thinking-toggle" @click="toggle">
      <ChevronDown
          :size="13" :stroke-width="2"
          class="chevron"
          :class="{ rotated: expanded }"
      />
      <span v-if="!thinkingStreaming">
        <Brain :size="12" :stroke-width="2" class="thinking-icon"/> 思考过程
      </span>
      <span v-else class="thinking-pending">
        <span class="thinking-text">
          <Brain :size="12" :stroke-width="2" class="thinking-icon thinking-pulse"/> 思考中<span
            class="dots-blink"><span>.</span><span>.</span><span>.</span></span>
        </span>
      </span>
    </button>
    <div
        v-if="expanded && cachedHtml"
        class="thinking-content markdown-body"
        v-html="cachedHtml"
    />
  </div>
</template>

<!-- 非 scoped 样式：供父容器 scoped 样式无法穿透的 thinking 区域使用 -->
<style>
/* ===== 思考区域 ===== */
.thinking-section {
  border-left: 2px solid var(--border-hover);
  padding-left: var(--space-4);
  margin-bottom: var(--space-1);
}

.thinking-toggle {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: .0625rem var(--space-2);
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  transition: background 0.15s, color 0.15s;
}

.thinking-toggle .thinking-icon {
  flex-shrink: 0;
  vertical-align: -0.125em;
}

.thinking-pulse {
  animation: atb-thinking-pulse 1.6s ease-in-out infinite;
  opacity: 0.7;
}

@keyframes atb-thinking-pulse {
  0%, 100% { opacity: 0.4; transform: scale(0.95); }
  50% { opacity: 1; transform: scale(1.05); }
}

.thinking-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-quaternary);
}

.chevron {
  transition: transform 0.2s;
}

.chevron.rotated {
  transform: rotate(0deg);
}

.chevron:not(.rotated) {
  transform: rotate(-90deg);
}

.thinking-content {
  font-size: var(--font-size-small);
  color: var(--text-quaternary);
  line-height: 1.5;
  word-break: break-word;
  padding: var(--space-2) var(--space-3);
  max-height: 18.75rem;
  overflow-y: auto;
}

.thinking-pending {
  font-size: var(--font-size-xs);
}

.thinking-text {
  animation: atb-thinking-breathe 1.8s ease-in-out infinite;
}

@keyframes atb-thinking-breathe {
  0%, 100% { color: var(--text-tertiary); }
  50% { color: var(--text-quaternary); }
}

.dots-blink {
  display: inline-block;
}

.dots-blink span {
  animation: atb-dot-bounce 1.2s ease-in-out infinite both;
  display: inline-block;
}

.dots-blink span:nth-child(2) {
  animation-delay: 0.15s;
}

.dots-blink span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes atb-dot-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.3; }
  30% { transform: translateY(-3px); opacity: 1; }
}
</style>
