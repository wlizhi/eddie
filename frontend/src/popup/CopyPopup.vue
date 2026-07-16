<!--
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 复制弹窗组件 — 折叠原文 + 自动复制到剪贴板 + 成功提示
 * 无需 AI 调用
-->

<template>
  <div class="content">
    <div class="collapse-section">
      <div class="collapse-header" @click="collapsed = !collapsed">
        <n-tooltip trigger="hover" placement="top" :show-arrow="false">
          <template #trigger>
            <span class="collapse-icon" :class="{rotated: !collapsed}">&#9654;</span>
          </template>
          原文
        </n-tooltip>
      </div>
      <div v-show="!collapsed" class="collapse-body">
        <div class="sel-text">{{ data.text }}</div>
      </div>
    </div>
    <div class="result-section">
      <div class="success-msg">
        <div class="check-icon">
          <svg width="3em" height="3em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5"/>
          </svg>
        </div>
        <div class="msg-text">已复制到剪贴板</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted} from 'vue'
import {NTooltip} from 'naive-ui'

const props = defineProps<{ data: { text: string } }>()
const collapsed = ref(true)

onMounted(() => {
  window.selectionAPI?.copyToClipboard(props.data.text)
})
</script>
