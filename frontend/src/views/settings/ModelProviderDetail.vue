<template>
  <div class="provider-detail">
    <!-- 属性设置 -->
    <div class="props-section">
      <div class="prop-row">
        <label class="prop-label">Code</label>
        <n-input
            :value="provider.code"
            disabled
        />
      </div>
      <div class="prop-row">
        <label class="prop-label">名称</label>
        <n-input
            :value="name"
            :disabled="provider.builtIn === 1"
            @update:value="$emit('update:name', $event)"
            @blur="$emit('save')"
        />
      </div>
      <div class="prop-row">
        <label class="prop-label">Base URL</label>
        <n-input
            :value="baseUrl"
            @update:value="$emit('update:baseUrl', $event)"
            @blur="$emit('save')"
        />
      </div>
      <div class="prop-row">
        <label class="prop-label">API Key</label>
        <n-input
            :value="apiKey"
            type="password"
            @update:value="$emit('update:apiKey', $event)"
            @blur="$emit('save')"
        />
      </div>
      <div class="delete-provider-row">
        <NButton
            v-if="provider.builtIn !== 1"
            type="error"
            ghost
            size="small"
            @click="$emit('delete-provider')"
        >
          <template #icon>
            <Trash2 :size="13"/>
          </template>
          删除
        </NButton>
      </div>
    </div>

    <!-- 分隔线 + 操作按钮 -->
    <div class="section-divider">
      <span class="divider-line"/>
      <button
          class="fetch-btn"
          @click="$emit('fetch-models')"
      >
        <RefreshCw :size="14" :stroke-width="1.8"/>
        获取模型列表
      </button>
    </div>

    <!-- 已添加模型列表 -->
    <div class="models-section">
      <div
          v-for="m in models"
          :key="m.code"
          class="model-row"
      >
        <span class="model-name">{{ m.code }}</span>
        <!-- 能力标签 -->
        <div class="model-capabilities" v-if="m.capabilities?.length">
          <span
              v-for="cap in m.capabilities"
              :key="cap"
              class="cap-tag"
              :class="cap"
          >
            <span v-html="capIcon(cap, 11)"></span>
            {{ CAPABILITY_LABELS[cap] || cap }}
          </span>
        </div>
        <button
            class="btn-settings"
            title="模型设置"
            @click="$emit('open-settings', m)"
        >
          <Settings :size="14" :stroke-width="1.8"/>
        </button>
        <button
            class="btn-remove"
            title="移除模型"
            @click="$emit('remove-model', m.code)"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
      </div>
      <div v-if="models.length === 0" class="models-empty">
        暂无已添加的模型，点击上方"获取模型列表"拉取
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {NButton, NInput} from 'naive-ui'
import {RefreshCw, Settings, Trash2} from '@lucide/vue'
import type {ModelItem, ModelProvider} from '@/types/modelProvider'
import {CAPABILITY_LABELS} from '@/types/modelProvider'
import {capIcon, normalizeCaps} from './modelCapabilities'

const props = defineProps<{
  provider: ModelProvider
  name: string
  baseUrl: string
  apiKey: string
}>()

defineEmits<{
  'update:name': [v: string]
  'update:baseUrl': [v: string]
  'update:apiKey': [v: string]
  'save': []
  'fetch-models': []
  'remove-model': [code: string]
  'open-settings': [m: ModelItem]
  'delete-provider': []
}>()

/** 模板直接使用的模型列表，capabilities 统一转小写 */
const models = computed(() =>
    (props.provider.models ?? []).map(m => ({
      ...m,
      capabilities: normalizeCaps(m.capabilities),
    }))
)
</script>

<style scoped src="./model-provider.css"></style>
