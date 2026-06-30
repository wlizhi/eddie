<!--
 * @author Eddie
 * @date 2026-06-21
-->

<!--
  InputArea.vue — 底部输入区域（编排器）

  根据 isMobile 分发到 Desktop / Mobile 视图：
  - Desktop 端：InputAreaDesktop.vue（模型选择器 + NPopselect + NSelect）
  - Mobile 端：InputAreaMobile.vue（BottomSheet 交互模式）
-->
<script setup lang="ts">
import {ref} from 'vue'
import {useMobile} from '@/composables/useMobile'
import InputAreaDesktop from './InputAreaDesktop.vue'
import InputAreaMobile from './InputAreaMobile.vue'

const {isMobile} = useMobile()

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
}>()

function onUpdate(val: string) {
  emit('update:modelValue', val)
}

function onSend() {
  emit('send')
}

/** 子组件 ref，用于暴露 focusInput */
const desktopRef = ref<InstanceType<typeof InputAreaDesktop> | null>(null)
const mobileRef = ref<InstanceType<typeof InputAreaMobile> | null>(null)

function focusInput() {
  if (isMobile.value) {
    mobileRef.value?.focusInput()
  } else {
    desktopRef.value?.focusInput()
  }
}

defineExpose({focusInput})
</script>

<template>
  <InputAreaDesktop
      v-if="!isMobile"
      ref="desktopRef"
      :model-value="modelValue"
      @update:model-value="onUpdate"
      @send="onSend"
  />
  <InputAreaMobile
      v-else
      ref="mobileRef"
      :model-value="modelValue"
      @update:model-value="onUpdate"
      @send="onSend"
  />
</template>
