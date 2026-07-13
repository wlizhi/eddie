<!--
 * @author Eddie
 * @date 2026-07-13
 *
 * AgentToolCard — 工具调用卡片
 *
 * 独立的工具执行结果卡片组件。每个卡片拥有自己的 expanded 折叠状态，
 * 展开时才渲染 Markdown 内容（v-if 控制），折叠时 renderMd 完全不执行。
 * computed 缓存 renderMd(buildToolContent(...)) 结果，避免重复计算。
 * 通过事件向父组件传递审批操作。
 -->
<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {ChevronDown} from '@lucide/vue'
import {renderMd} from '@/utils/markdown'
import {formatToolResult} from '@/utils/tool-result'
import type {ToolExecutionRecord} from '@/types/chat'

const props = defineProps<{
  /** 工具执行记录（含 toolName、arguments、result、done 等） */
  toolCall: ToolExecutionRecord
  /** 渲染单元唯一标识（用于防重复渲染） */
  unitKey?: string
  /** 父组件标记此单元是否已完成首次展开渲染 */
  unitRendered?: boolean
}>()

const emit = defineEmits<{
  approve: [approved: boolean]
  rendered: [unitKey: string]
}>()

/** 自身展开/折叠状态 */
const expanded = ref(false)

function toggle() {
  expanded.value = !expanded.value
}

/** 工具名 → 友好显示名 */
function displayToolName(toolName: string): string {
  return toolName
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase())
}

/** 构建工具调用内容的 Markdown：参数 + 结果 */
function buildToolContent(args: string | undefined, result: string | undefined): string {
  let content = ''
  if (args) {
    content += '**→ 参数**\n\n'
    try {
      const parsed = JSON.parse(args)
      content += '```json\n' + JSON.stringify(parsed, null, 2) + '\n```'
    } catch {
      content += '```\n' + args + '\n```'
    }
  }
  if (result) {
    if (content) content += '\n\n'
    content += '**← 结果**\n\n'
    content += formatToolResult(result)
  }
  return content
}

/** 缓存 Markdown 渲染结果 — 按 unitKey 做缓存，工具执行完成前可重建 */
let renderedHtmlCache = ''
let cacheUnitKey = ''
let cacheIsFinal = false
const renderedContent = computed(() => {
  if (!expanded.value) return ''
  if (!props.toolCall.arguments && !props.toolCall.result) return ''

  // unitKey 匹配且缓存内容已是最终态 → 命中，不再重复渲染
  if (renderedHtmlCache && cacheIsFinal && props.unitKey === cacheUnitKey) {
    return renderedHtmlCache
  }

  renderedHtmlCache = renderMd(buildToolContent(props.toolCall.arguments, props.toolCall.result))
  cacheUnitKey = props.unitKey || ''
  cacheIsFinal = !!props.toolCall.done
  return renderedHtmlCache
})

/** 首次展开且工具已执行完成（内容稳定）→ 发射 rendered 信号 */
watch(expanded, (val) => {
  if (val && !props.unitRendered && props.unitKey && props.toolCall.done) {
    // 强制 computed 先求值并缓存渲染结果，再发射标记
    // 避免 pre-flush 时序导致 unitRendered 先于缓存生效
    renderedContent.value
    emit('rendered', props.unitKey)
  }
})

/** 状态图标 */
const statusIcon = computed(() => {
  const tc = props.toolCall
  if (tc.done) return tc.error ? '✕' : (tc.rejected ? '⚠' : '✓')
  return tc.pendingApproval ? '⏳' : '⟳'
})

/** 状态文本 */
const statusText = computed(() => {
  const tc = props.toolCall
  if (!tc.done) return tc.pendingApproval ? '等待审批...' : '运行中...'
  return ''
})

/** 卡片修饰 class */
const cardClass = computed(() => ({
  'tool-error': !!props.toolCall.error,
  'tool-rejected': !!props.toolCall.rejected,
}))
</script>

<template>
  <div
      class="tool-execution-card"
      :class="cardClass"
  >
    <div class="tool-execution-header" @click="toggle">
      <ChevronDown :size="12" :stroke-width="2" class="tool-chevron"
                   :class="{ rotated: expanded }"/>
      <span class="tool-execution-icon">{{ statusIcon }}</span>
      <span class="tool-execution-name">{{ displayToolName(toolCall.toolName) }}</span>
      <span v-if="!toolCall.done" class="tool-execution-status"
            :class="{ 'tool-status-pending': toolCall.pendingApproval }">
        {{ statusText }}
      </span>
    </div>

    <!-- 工具结果内容：展开时才渲染，标记后返回缓存，折叠再展开不重复计算 -->
    <div
        v-if="expanded && renderedContent"
        class="tool-execution-result markdown-body"
        v-html="renderedContent"
    />

    <!-- 审批按钮 -->
    <div v-if="toolCall.pendingApproval" class="tool-approval-actions">
      <button class="tool-approve-btn" @click.stop="emit('approve', true)">✓ 批准</button>
      <button class="tool-reject-btn" @click.stop="emit('approve', false)">✕ 拒绝</button>
    </div>
  </div>
</template>

<!-- 非 scoped 样式：供父组件渲染的 tool-calls-section 容器及本组件使用 -->
<style>
/* ===== 工具执行卡片（气泡内 inline） ===== */
.tool-calls-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: var(--space-2) 0;
}

.tool-execution-card {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-base);
  border-radius: 6px;
  padding: var(--space-2) var(--space-4);
  font-size: var(--font-size-xs);
  color: var(--text-quaternary);
}

.tool-execution-card.tool-error {
  border-color: var(--danger-default);
  color: var(--danger-default);
}

.tool-execution-card.tool-rejected {
  border-color: var(--warning-default);
  color: var(--warning-default);
}

.tool-execution-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  user-select: none;
}

.tool-execution-header:hover {
  opacity: 0.8;
}

.tool-chevron {
  transition: transform 0.2s;
  flex-shrink: 0;
}

.tool-chevron.rotated {
  transform: rotate(0deg);
}

.tool-chevron:not(.rotated) {
  transform: rotate(-90deg);
}

.tool-execution-icon {
  flex-shrink: 0;
  width: 1rem;
  text-align: center;
  font-size: var(--font-size-xs);
}

.tool-execution-card.tool-error .tool-execution-icon {
  color: var(--danger-default);
}

.tool-execution-name {
  font-weight: 500;
}

.tool-execution-status {
  font-size: var(--font-size-xxs);
  color: var(--text-muted);
  animation: atc-tool-pulse 1.5s ease-in-out infinite;
}

@keyframes atc-tool-pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.tool-status-pending {
  color: var(--accent-default);
  animation: atc-pending-pulse 1.6s ease-in-out infinite;
}

@keyframes atc-pending-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.tool-execution-result {
  margin-top: var(--space-2);
  padding: var(--space-2);
  background: var(--bg-secondary);
  border-radius: 4px;
  font-size: var(--font-size-xs);
  overflow-y: auto;
  max-height: 18.75rem;
}

.tool-approval-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--border-base);
}

.tool-approve-btn,
.tool-reject-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border-radius: 4px;
  border: 1px solid var(--border-base);
  cursor: pointer;
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  transition: background 0.15s, border-color 0.15s, color 0.15s;
  user-select: none;
  line-height: 1.4;
}

.tool-approve-btn:hover {
  background: color-mix(in srgb, var(--success-default) 10%, transparent);
  border-color: var(--success-default);
  color: var(--success-default);
}

.tool-reject-btn:hover {
  background: color-mix(in srgb, var(--danger-default) 10%, transparent);
  border-color: var(--danger-default);
  color: var(--danger-default);
}

.tool-approve-btn:active,
.tool-reject-btn:active {
  transform: scale(0.96);
}
</style>
