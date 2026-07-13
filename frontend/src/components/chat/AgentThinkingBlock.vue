<!--
 * @author Eddie
 * @date 2026-07-13
 *
 * AgentThinkingBlock — 思考过程块
 *
 * 独立的 thinking 折叠/展开组件。每个 thinking 块拥有自己的 expanded 状态，
 * 展开时才渲染 Markdown 内容（v-if 控制），折叠时 renderMd 完全不执行。
 * 思考中状态（isStreaming && isLast && showPending）显示 "思考中..." 动画。
 -->
<script setup lang="ts">
import {computed, ref} from 'vue'
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
}>()

/** 自身展开/折叠状态，不依赖父组件 */
const expanded = ref(false)

/** 切换展开/折叠 */
function toggle() {
  expanded.value = !expanded.value
}

/** 缓存 Markdown 渲染结果 */
const renderedThinking = computed(() => renderMd(props.thinking))

/** 是否显示"思考中..."占位：正在流式 + 最新消息 + 无 content + 有 thinking 内容（正在思考中） */
const showPending = computed(() =>
    props.isStreaming && props.isLast && !props.hasContent
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
      <span v-if="hasContent || !isStreaming || !isLast">
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
        v-if="expanded && thinking"
        class="thinking-content markdown-body"
        v-html="renderedThinking"
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
