<!--
 * @author Eddie
 * @date 2026-06-28
-->

<!--
  PromptVariablePanel.vue — 系统提示词模板变量选择面板

  功能：
  - 自动加载 GET /api/system/prompt-variables 获取支持的变量列表
  - NCollapse 折叠面板展示，默认收起
  - NTag 标签展示每个变量，hover 时 NTooltip 显示描述+示例值
  - 点击标签触发 @insert 事件，父组件处理插入逻辑

  用法：
    <PromptVariablePanel @insert="(tpl) => myPrompt += tpl" />

  未来智能体页面可直接复用，无需关心变量加载细节。
-->
<script setup lang="ts">
import {NCollapse, NCollapseItem, NTag, NTooltip} from 'naive-ui'
import {Code} from '@lucide/vue'
import {onMounted, ref} from 'vue'
import {fetchPromptVariables} from '@/api/assistant'
import type {PromptVariableInfo} from '@/types/assistant'
import {TIP_THEME_OVERRIDES} from '@/constants/theme'

const emit = defineEmits<{
  insert: [template: string]
}>()

const variables = ref<PromptVariableInfo[]>([])
const expanded = ref(false)

onMounted(async () => {
  try {
    variables.value = await fetchPromptVariables()
  } catch {
    variables.value = []
  }
})
</script>

<template>
  <NCollapse
      v-if="variables.length > 0"
      :expanded-names="expanded ? ['variables'] : []"
      :on-update:expanded-names="(v: Array<string>) => expanded = v.length > 0"
      :theme-overrides="{ titlePadding: '6px 10px' }"
      class="pv-panel"
  >
    <NCollapseItem name="variables">
      <template #header>
        <div class="pv-header">
          <Code :size="14" :stroke-width="2"/>
          <span>支持的变量</span>
          <span class="pv-count">({{ variables.length }})</span>
        </div>
      </template>
      <div class="pv-list">
        <NTooltip
            v-for="v in variables" :key="v.key"
            :theme-overrides="TIP_THEME_OVERRIDES"
            :show-arrow="false"
            placement="top"
        >
          <template #trigger>
            <NTag
                :bordered="false"
                size="tiny"
                clickable
                @click="emit('insert', v.template)"
                class="pv-tag"
            >
              {{ v.template }}
            </NTag>
          </template>
          <div class="pv-tooltip-body">
            <div>{{ v.description }}</div>
            <div class="pv-tooltip-example">示例: {{ v.example }}</div>
          </div>
        </NTooltip>
      </div>
    </NCollapseItem>
  </NCollapse>
</template>

<style src="./prompt-variable-panel.css" scoped/>
