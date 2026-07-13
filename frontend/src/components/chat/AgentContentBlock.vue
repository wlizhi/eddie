<!--
 * @author Eddie
 * @date 2026-07-13
 *
 * AgentContentBlock — 消息内容块
 *
 * 独立渲染消息的 Markdown 正文内容，带 renderMd 缓存。
 * 使用 computed 缓存渲染结果，避免父组件重渲染时重复调用 renderMd。
 * 内容始终展开显示，不折叠。
 -->
<script setup lang="ts">
import {computed} from 'vue'
import {renderMd} from '@/utils/markdown'

const props = defineProps<{
  content: string
}>()

/** 缓存 Markdown 渲染结果 */
const renderedContent = computed(() => {
  if (!props.content) return ''
  return renderMd(props.content)
})
</script>

<template>
  <div
      v-if="content"
      class="message-content markdown-body"
      v-html="renderedContent"
  />
</template>
