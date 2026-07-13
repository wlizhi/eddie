<!--
 * @author Eddie
 * @date 2026-07-06
 * 
 * AgentPlanTodoList.vue — 任务计划待办清单
 * 
 * 展示规划模式下的进度条 + 待办列表。
 * 接收 taskPlan 数据，渲染进度横条（带节点）和待办清单。
 * 纯展示组件，内部无流式逻辑。
-->

<script setup lang="ts">
import {computed} from 'vue'
import type {AgentTaskPlan} from '@/types/agent-chat'
import {Check, Circle, Loader, X} from '@lucide/vue'

const props = defineProps<{
  plan: AgentTaskPlan
}>()

/** 已完成数量 */
const doneCount = computed(() =>
    props.plan.steps.filter(t => t.status === 'completed').length
)

/** 总数量 */
const totalCount = computed(() => props.plan.steps.length)

/** 状态文本 */
const statusText = computed(() => {
  switch (props.plan.status) {
    case 'planned':
      return '已规划'
    case 'executing':
      return '执行中'
    case 'completed':
      return '已完成'
    case 'failed':
      return '已失败'
    default:
      return props.plan.status
  }
})

/** 状态颜色变量 */
function statusColor(status: string): string {
  switch (status) {
    case 'completed':
      return 'var(--success-default)'
    case 'processing':
      return 'var(--accent-default)'
    case 'failed':
      return 'var(--danger-default)'
    default:
      return 'var(--text-muted)'
  }
}

/** 状态图标 */
function statusIcon(status: string): string {
  switch (status) {
    case 'completed':
      return 'completed'
    case 'processing':
      return 'processing'
    case 'failed':
      return 'failed'
    default:
      return 'pending'
  }
}
</script>

<template>
  <div class="plan-todo-list">
    <!-- 标题 -->
    <div class="plan-header">
      <span class="plan-title-text">{{ plan.title }}</span>
      <span class="plan-status-tag" :class="'status-' + plan.status" :style="{ color: statusColor(plan.status) }">
        {{ statusText }}
      </span>
    </div>

    <!-- 进度横条 + 节点 -->
    <div class="plan-bar">
      <div class="plan-bar-track">
        <div
            v-for="(todo, idx) in plan.steps"
            :key="todo.stepNumber"
            class="plan-bar-node"
            :class="todo.status"
            :style="{ zIndex: totalCount - idx }"
        >
          <div class="node-dot" :class="statusIcon(todo.status)">
            <Check v-if="todo.status === 'completed'" :size="8" class="node-icon"/>
            <Loader v-else-if="todo.status === 'processing'" :size="8" class="node-icon processing-icon"/>
            <X v-else-if="todo.status === 'failed'" :size="8" class="node-icon"/>
            <Circle v-else :size="6" class="node-icon pending-icon"/>
          </div>
          <span class="node-label">Step {{ todo.stepNumber }}</span>
        </div>
      </div>
      <span class="plan-bar-count">{{ doneCount }}/{{ totalCount }}</span>
    </div>

    <!-- 待办清单 -->
    <div class="plan-todos">
      <div
          v-for="todo in plan.steps"
          :key="todo.stepNumber"
          class="plan-todo-item"
          :class="todo.status"
      >
        <span class="todo-dot" :class="todo.status"/>
        <span class="todo-step-label">Step {{ todo.stepNumber }}</span>
        <span class="todo-desc">{{ todo.title || todo.description }}</span>
      </div>
    </div>
  </div>
</template>

<style src="./agent-plan-todo-list.css" scoped/>
