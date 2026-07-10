<!--
 * @author Eddie
 * @date 2026-06-27
-->

<template>
  <div class="mps-params-grid">
    <div class="mps-param-item" v-for="def in paramDefs" :key="def.key">
      <span class="mps-param-label">
        {{ def.label }}
        <NTooltip trigger="hover" placement="top" :show-arrow="false" :theme-overrides="tipTheme">
          <template #trigger>
            <span class="mps-hint-icon">ⓘ</span>
          </template>
          {{ def.tip }}
        </NTooltip>
      </span>
      <n-input-number
          v-if="def.componentType !== 'select'"
          :value="localParams[def.key] as number | null"
          :step="def.step"
          :min="def.min"
          :max="def.max"
          :status="getFieldStatus(def.key)"
          class="mps-param-input"
          placeholder="默认"
          @update:value="(v: number | null) => onValueChange(def, v)"
          @blur="onBlur(def)"
      />
      <NSelect
          v-else
          :value="localParams[def.key] as string"
          :options="def.options"
          :consistent-menu-width="false"
          class="mps-param-input"
          @update:value="(v: string) => onSelectChange(def, v)"
      />
      <span v-if="errorMessages[def.key]" class="mps-error-msg">{{ errorMessages[def.key] }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, watch} from 'vue'
import {NInputNumber, NSelect, NTooltip} from 'naive-ui'
import type {ModelParamDef} from '@/constants/modelParams'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'
import {TIP_THEME_OVERRIDES} from '@/constants/theme'

const tipTheme = TIP_THEME_OVERRIDES

/**
 * 模型参数输入通用组件
 *
 * 支持 v-model 绑定参数对象，自动校验范围，显示错误状态和悬浮提示。
 *
 * 用法：
 *   <ModelParamsInput v-model:params="myParams" />
 *
 * 父组件可通过 ref 调用 validate() 或检查 hasErrors 来阻止提交：
 *   const paramsRef = ref<InstanceType<typeof ModelParamsInput>>()
 *   const valid = paramsRef.value?.validate() ?? true
 */
const props = withDefaults(defineProps<{
  params: Record<string, any>
  paramDefs?: ModelParamDef[]
}>(), {
  paramDefs: () => MODEL_PARAM_DEFS,
})

const emit = defineEmits<{
  'update:params': [value: Record<string, any>]
  error: [hasError: boolean]
  blur: []
}>()

/** 内部参数副本，避免直接修改 props */
const localParams = reactive<Record<string, any>>({...props.params})

/** 错误信息：key → 错误文本 */
const errorMessages = reactive<Record<string, string | null>>({})

/** 同步外部 props 变化 */
watch(() => props.params, (val) => {
  for (const key of Object.keys(localParams)) {
    localParams[key] = val[key] ?? null
  }
}, {deep: true})

/** 校验单个参数，返回错误文本或 null（仅对 number 类型生效） */
function validateOne(def: ModelParamDef, value: any): string | null {
  if (def.componentType === 'select') return null
  if (value === null) return null
  if (def.min !== undefined && value < def.min) {
    return `最小值 ${def.min}`
  }
  if (def.max !== undefined && value > def.max) {
    return `最大值 ${def.max}`
  }
  return null
}

/** 获取输入框的状态 */
function getFieldStatus(key: string): 'error' | undefined {
  return errorMessages[key] ? 'error' : undefined
}

/** 数字输入值变化时同步并校验 */
function onValueChange(def: ModelParamDef, v: number | null) {
  localParams[def.key] = v
  errorMessages[def.key] = validateOne(def, v)
  emitErrorState()
  emitParams()
}

/** 下拉选择变化时同步，并通知父组件触发保存 */
function onSelectChange(def: ModelParamDef, v: string) {
  localParams[def.key] = v
  errorMessages[def.key] = null
  emitErrorState()
  emitParams()
  emit('blur')
}

/** 失焦时重新校验（兜底，仅对 number 类型生效），并通知父组件触发保存 */
function onBlur(def: ModelParamDef) {
  if (def.componentType === 'select') return
  const v = localParams[def.key]
  const msg = validateOne(def, v)
  if (msg !== errorMessages[def.key]) {
    errorMessages[def.key] = msg
    emitErrorState()
  }
  emit('blur')
}

/** 是否有参数校验错误 */
const hasErrors = computed(() =>
    Object.values(errorMessages).some(msg => msg !== null)
)

function emitErrorState() {
  emit('error', hasErrors.value)
}

function emitParams() {
  emit('update:params', {...localParams})
}

/**
 * 手动触发全量校验，返回 true = 校验通过
 * 父组件可在提交前调用
 */
function validate(): boolean {
  for (const def of props.paramDefs) {
    errorMessages[def.key] = validateOne(def, localParams[def.key])
  }
  emitErrorState()
  return !hasErrors.value
}

defineExpose({validate, hasErrors})
</script>

<style scoped>
.mps-params-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.mps-param-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mps-param-label {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  white-space: nowrap;
}

.mps-param-input {
  width: 100%;
}

/* 统一 NInputNumber 与 NSelect 底部间距，消除视觉对齐偏差 */
.mps-param-input :deep(.n-base-selection) {
  margin-bottom: 2px;
}

.mps-hint-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 13px;
  height: 13px;
  font-size: var(--font-size-xxs);
  color: var(--text-muted);
  cursor: pointer;
  margin-left: 3px;
  vertical-align: middle;
  line-height: 1;
  transition: color 0.15s;
}

.mps-hint-icon:hover {
  color: var(--text-quaternary);
}

.mps-error-msg {
  font-size: var(--font-size-xs);
  color: var(--danger-default);
  margin-top: 1px;
  line-height: 1.3;
  min-height: 16px;
}
</style>
