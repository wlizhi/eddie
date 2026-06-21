<script setup lang="ts">
import {computed, ref} from 'vue'
import ImageCropper from './ImageCropper.vue'

/** 常见 Emoji 列表 */
const EMOJI_LIST = [
  '😀', '😂', '🤣', '😊', '😍', '🤩', '😎', '🤔',
  '🤗', '😴', '😮', '😅', '😆', '😉', '😋', '😜',
  '🤖', '👻', '💀', '👽', '🎃', '😺', '🙀', '🐱',
  '❤️', '💛', '💚', '💙', '💜', '🖤', '💕', '💖',
  '🌟', '⭐', '🔥', '💡', '🎯', '🚀', '🎉', '🎊',
  '🌈', '🌻', '🌸', '🍀', '🌺', '🌙', '☀️', '❄️',
  '⚡', '💎', '👑', '🎵', '🎶', '📚', '✏️', '🎨',
  '🍕', '🍔', '🌮', '🍦', '☕', '🍵', '🍺', '🍷',
]

const props = withDefaults(defineProps<{
  currentAvatar?: string | null
}>(), {
  currentAvatar: null,
})

const emit = defineEmits<{
  confirm: [value: string | null, file: File | null]
  close: []
}>()

type Tab = 'text' | 'emoji' | 'upload'
const activeTab = ref<Tab>('text')
const inputText = ref(props.currentAvatar ?? '')
const selectedFile = ref<File | null>(null)
const croppedBlob = ref<Blob | null>(null)
const croppedUrl = computed(() => {
  if (!croppedBlob.value) return ''
  return URL.createObjectURL(croppedBlob.value)
})
const showCropper = ref(false)

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
  showCropper.value = false
  selectedFile.value = null
}

function handleConfirm() {
  if (activeTab.value === 'upload' && croppedBlob.value) {
    const file = new File([croppedBlob.value], 'avatar.webp', {type: 'image/webp'})
    emit('confirm', null, file)
  } else if (inputText.value.trim()) {
    emit('confirm', inputText.value.trim(), null)
  }
}

function selectEmoji(emoji: string) {
  inputText.value = emoji
  activeTab.value = 'text'
}

function reset() {
  inputText.value = props.currentAvatar ?? ''
  selectedFile.value = null
  croppedBlob.value = null
  showCropper.value = false
  activeTab.value = 'text'
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
          <span class="preview-label">预览：</span>
          <span class="preview-char">{{ inputText || '?' }}</span>
        </div>
      </div>

      <div v-show="activeTab === 'emoji'" class="emoji-tab">
        <div class="emoji-grid">
          <button v-for="emoji in EMOJI_LIST" :key="emoji"
                  class="emoji-item" :class="{selected: inputText === emoji}"
                  @click="selectEmoji(emoji)">{{ emoji }}
          </button>
        </div>
      </div>

      <div v-show="activeTab === 'upload'" class="upload-tab">
        <div v-if="!selectedFile" class="upload-placeholder">
          <label class="upload-btn">
            选择图片
            <input type="file" accept="image/*" hidden @change="onFileSelected"/>
          </label>
        </div>
        <ImageCropper v-if="selectedFile && showCropper" :file="selectedFile"
                      @cropped="onCropped" @cancel="onCropCancel"/>
        <div v-if="croppedBlob && !showCropper" class="cropped-preview">
          <img :src="croppedUrl" alt="裁剪结果" class="preview-img"/>
          <p class="cropped-hint">图片已裁剪</p>
          <div class="preview-actions">
            <button class="btn-rechoose" @click="showCropper = true">重新裁剪</button>
            <button class="btn-rechoose" @click="selectedFile = null; croppedBlob = null">换张图片</button>
          </div>
        </div>
      </div>
    </div>

    <div class="footer">
      <button class="btn btn-cancel" @click="emit('close')">取消</button>
      <button class="btn btn-confirm" :disabled="!inputText.trim() && !croppedBlob" @click="handleConfirm">确认</button>
    </div>
  </div>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
}

.tab {
  flex: 1;
  padding: 7px 0;
  border: none;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-family: inherit;
  transition: color 0.15s, border-color 0.15s;
}

.tab:hover {
  color: #6b7280;
}

.tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
}

.tab-content {
  min-height: 180px;
}

.text-tab {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
}

.text-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e2e6;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.text-input:focus {
  border-color: #2563eb;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-label {
  font-size: 12px;
  color: #9ca3af;
}

.preview-char {
  font-size: 36px;
  line-height: 1;
}

.emoji-tab {
  max-height: 220px;
  overflow-y: auto;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 4px 0;
}

.emoji-item {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 22px;
  cursor: pointer;
  transition: background 0.12s;
  padding: 0;
}

.emoji-item:hover {
  background: #f0f1f3;
}

.emoji-item.selected {
  background: #e0e7ff;
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}

.upload-tab {
  padding: 8px 0;
}

.upload-placeholder {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 24px;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
}

.upload-btn:hover {
  background: #1d4ed8;
}

.cropped-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.preview-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e5e7eb;
}

.cropped-hint {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.btn-rechoose {
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #e0e2e6;
  background: #f4f5f7;
  color: #6b7280;
  transition: background 0.15s;
}

.btn-rechoose:hover {
  background: #e8eaee;
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
}

.btn {
  padding: 6px 18px;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s, opacity 0.15s;
}

.btn-cancel {
  background: #f4f5f7;
  color: #6b7280;
  border-color: #e0e2e6;
}

.btn-cancel:hover {
  background: #e8eaee;
}

.btn-confirm {
  background: #2563eb;
  color: #fff;
}

.btn-confirm:hover {
  background: #1d4ed8;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
