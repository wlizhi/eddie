<script setup lang="ts">
import {computed, nextTick, ref, watch} from 'vue'
import ImageCropper from './ImageCropper.vue'

import {EMOJI_GROUPS} from '@/constants/emojis'

const props = withDefaults(defineProps<{
  currentAvatar?: string | null
}>(), {
  currentAvatar: null,
})

const emit = defineEmits<{
  confirm: [value: string | null, file: File | null]
  close: []
}>()

/** 判断是否为 emoji */
function isEmoji(s: string): boolean {
  if (s.length > 2) return false
  const cp = s.codePointAt(0) ?? 0
  return (cp >= 0x1F300 && cp <= 0x1F9FF) || (cp >= 0x2600 && cp <= 0x27BF)
      || (cp >= 0xFE00 && cp <= 0xFE0F) || cp === 0x200D
      || (cp >= 0x1FA00 && cp <= 0x1FA6F) || (cp >= 0x1F600 && cp <= 0x1F64F)
      || (cp >= 0x2702 && cp <= 0x27B0)
}

/** 判断是否为图片 URL */
function isImageUrl(s: string): boolean {
  return s.startsWith('http://') || s.startsWith('https://') || s.startsWith('/') || s.startsWith('blob:') || s.startsWith('data:')
}

/** 根据当前头像值推断应显示的 Tab */
function detectTab(avatar: string | null | undefined): { tab: 'text' | 'emoji' | 'upload'; text: string } {
  if (!avatar) return {tab: 'text', text: ''}
  if (isImageUrl(avatar)) return {tab: 'upload', text: ''}
  if (isEmoji(avatar)) return {tab: 'emoji', text: avatar}
  if ([...avatar].length === 1) return {tab: 'text', text: avatar}
  return {tab: 'text', text: avatar}
}

type Tab = 'text' | 'emoji' | 'upload'
const initTab = detectTab(props.currentAvatar)
const activeTab = ref<Tab>(initTab.tab)
const inputText = ref(initTab.text)

// 当外部 currentAvatar 变化时同步
watch(() => props.currentAvatar, (v) => {
  const r = detectTab(v)
  activeTab.value = r.tab
  inputText.value = r.text
  selectedFile.value = null
  croppedBlob.value = null
  showCropper.value = false
})
const selectedFile = ref<File | null>(null)
const croppedBlob = ref<Blob | null>(null)
const croppedUrl = computed(() => {
  if (!croppedBlob.value) return ''
  return URL.createObjectURL(croppedBlob.value)
})
/** 当前头像是否为图片 URL */
const currentAvatarUrl = computed(() => {
  if (!props.currentAvatar) return null
  return isImageUrl(props.currentAvatar) ? props.currentAvatar : null
})
const showCropper = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

/** 直接触发文件选择器 */
async function pickImage() {
  selectedFile.value = null
  croppedBlob.value = null
  showCropper.value = false
  await nextTick()
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  selectedFile.value = file
  croppedBlob.value = null
  showCropper.value = true
}

function onCropped(blob: Blob) {
  croppedBlob.value = blob
  showCropper.value = false
}

function onCropCancel() {
  pickImage()
}

function handleConfirm() {
  if (activeTab.value === 'upload' && croppedBlob.value) {
    const file = new File([croppedBlob.value], 'avatar.webp', {type: 'image/webp'})
    emit('confirm', null, file)
  } else if (inputText.value.trim()) {
    emit('confirm', inputText.value.trim(), null)
  } else {
    // 空白输入 → 清空头像，走默认首字显示
    emit('confirm', null, null)
  }
}

function selectEmoji(emoji: string) {
  inputText.value = emoji
  activeTab.value = 'text'
}

function reset() {
  const r = detectTab(props.currentAvatar)
  activeTab.value = r.tab
  inputText.value = r.text
  selectedFile.value = null
  croppedBlob.value = null
  showCropper.value = false
}

defineExpose({reset})
</script>

<template>
  <div class="picker">
    <div class="tabs">
      <button :class="['tab', {active: activeTab === 'text'}]" @click="activeTab = 'text'">文字</button>
      <button :class="['tab', {active: activeTab === 'emoji'}]" @click="activeTab = 'emoji'">Emoji</button>
      <button :class="['tab', {active: activeTab === 'upload'}]" @click="activeTab = 'upload'">上传</button>
    </div>

    <div class="tab-content">
      <div v-show="activeTab === 'text'" class="text-tab">
        <input v-model="inputText" class="text-input" placeholder="输入文字或 Emoji" maxlength="2"/>
        <div class="preview-row">
          <div class="preview-circle" :style="{ background: inputText && !isEmoji(inputText) ? '#2563eb' : '#f3f4f6' }">
            <span v-if="isEmoji(inputText)" class="preview-emoji">{{ inputText || '?' }}</span>
            <span v-else class="preview-char">{{ inputText || '?' }}</span>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'emoji'" class="emoji-tab">
        <div v-for="group in EMOJI_GROUPS" :key="group.label" class="emoji-group">
          <div class="emoji-group-label">{{ group.label }}</div>
          <div class="emoji-grid">
            <button v-for="emoji in group.emojis" :key="emoji"
                    class="emoji-item" :class="{selected: inputText === emoji}"
                    @click="selectEmoji(emoji)">{{ emoji }}
            </button>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'upload'" class="upload-tab">
        <input ref="fileInputRef" type="file" accept="image/*" hidden @change="onFileSelected"/>
        <div v-if="!selectedFile && !currentAvatarUrl" class="upload-placeholder">
          <label class="upload-btn" @click="pickImage">选择图片</label>
        </div>
        <div v-if="!selectedFile && currentAvatarUrl" class="cropped-preview">
          <img :src="currentAvatarUrl" alt="当前头像" class="preview-img"/>
          <p class="cropped-hint">当前头像</p>
          <div class="preview-actions">
            <button class="btn-rechoose" @click="pickImage">换张图片</button>
          </div>
        </div>
        <ImageCropper v-if="selectedFile && showCropper" :file="selectedFile"
                      @cropped="onCropped" @cancel="onCropCancel"/>
        <div v-if="croppedBlob && !showCropper" class="cropped-preview">
          <img :src="croppedUrl" alt="裁剪结果" class="preview-img"/>
          <p class="cropped-hint">图片已裁剪</p>
          <div class="preview-actions">
            <button class="btn-rechoose" @click="showCropper = true">重新裁剪</button>
            <button class="btn-rechoose" @click="pickImage">换张图片</button>
          </div>
        </div>
      </div>
    </div>

    <div class="footer">
      <button class="btn btn-cancel" @click="emit('close')">取消</button>
      <button class="btn btn-confirm" @click="handleConfirm">确认</button>
    </div>
  </div>
</template>

<style src="./avatar-picker.css" scoped/>
