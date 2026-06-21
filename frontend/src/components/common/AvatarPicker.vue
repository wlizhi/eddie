<script setup lang="ts">
import {computed, nextTick, ref, watch} from 'vue'
import ImageCropper from './ImageCropper.vue'

const EMOJI_GROUPS: { label: string; emojis: string[] }[] = [
  {
    label: '表情',
    emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '😊', '😇', '😍', '🤩', '😘', '😗', '😚', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '😮', '😯', '😲', '😳', '🥺', '😢', '😭', '😤', '😡', '🤬', '😈', '👿', '💀', '☠️']
  },
  {
    label: '手势',
    emojis: ['👍', '👎', '👌', '🤞', '🤟', '🤘', '🤙', '✌️', '👋', '🤚', '✋', '🖐', '🖖', '👏', '🙌', '🤝', '💪', '🦾', '🦿', '🤳', '🙏', '✍️', '💅']
  },
  {label: '爱心', emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟']},
  {
    label: '动物',
    emojis: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🐤', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋', '🐌', '🐞', '🐜', '🦗', '🕷', '🦂', '🐢', '🐍', '🦎', '🦖', '🦕', '🐙', '🦑', '🦐', '🦀', '🐡', '🐠', '🐟', '🐬', '🐳', '🐋', '🦈', '🐊', '🐅', '🐆', '🦓', '🦍', '🐘', '🦏', '🐪', '🐫', '🦒', '🐃', '🐂', '🐄', '🐎', '🐖', '🐏', '🐑', '🐐', '🦌', '🐕', '🐩', '🐈', '🐓', '🦃', '🕊', '🐇', '🐁', '🐀', '🐿', '🦔']
  },
  {
    label: '自然',
    emojis: ['🌈', '☀️', '🌙', '⭐', '🌟', '✨', '🔥', '💧', '❄️', '☁️', '⛅', '🌧', '⛈', '🌩', '⚡', '💨', '🌪', '🌊', '🌱', '🌲', '🌳', '🌴', '🌵', '🌾', '🌿', '🍀', '🍁', '🍂', '🍃', '🌍', '🌎', '🌏', '🌕', '🌖', '🌗', '🌘', '🌑', '🌒', '🌓', '🌔', '🌚', '🌝', '🌞', '🌛', '🌜']
  },
  {label: '花卉', emojis: ['🌸', '🌼', '🌻', '🌺', '🌹', '🌷', '💐', '🥀', '💮', '🏵', '🌾']},
  {
    label: '水果',
    emojis: ['🍎', '🍏', '🍊', '🍋', '🍐', '🍑', '🍒', '🍓', '🥝', '🍅', '🥥', '🥑', '🍆', '🥔', '🥕', '🌽', '🌶', '🥒', '🥬', '🥦', '🍄', '🥜', '🌰', '🍇', '🍈', '🍉', '🍌', '🍍', '🥭', '🍑', '🍒']
  },
  {
    label: '食物',
    emojis: ['🍕', '🍔', '🌮', '🌯', '🥙', '🍟', '🍗', '🍖', '🥩', '🥓', '🧀', '🍳', '🥞', '🧇', '🍞', '🥐', '🥖', '🥨', '🥯', '🍜', '🍝', '🍣', '🍤', '🍚', '🍛', '🍲', '🥘', '🍿', '🧈', '🍦', '🍨', '🍩', '🍪', '🎂', '🍰', '🧁', '🍫', '🍬', '🍭', '🍮', '🍯', '☕', '🍵', '🍺', '🍻', '🍷', '🥂', '🍸', '🍹', '🧉', '🧊']
  },
  {
    label: '物品',
    emojis: ['💡', '🔑', '💎', '🎁', '📚', '✏️', '🎨', '📷', '📱', '💻', '⌚', '🎧', '🎮', '🔮', '🧿', '🪄', '💣', '🧨', '🔪', '🏹', '🛡', '🧲', '⚖️', '🔗', '🧰', '🧲', '🧪', '🧫', '🧬', '🩺', '💊', '🩹', '🩼', '🧽']
  },
  {
    label: '运动',
    emojis: ['⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🏉', '🎱', '🏓', '🏸', '🏒', '🏑', '🥍', '🏏', '⛳', '🎣', '🤿', '🎽', '🎿', '🛷', '🥌', '🎯', '🪀', '🪁', '🏹']
  },
  {label: '音乐', emojis: ['🎵', '🎶', '🎼', '🎤', '🎧', '🎷', '🎸', '🎹', '🎺', '🎻', '🥁', '🪘', '🎬']},
  {label: '庆祝', emojis: ['🎉', '🎊', '🎈', '🎀', '🎁', '🏆', '🥇', '🥈', '🥉', '🎖', '🏅', '🎗', '🎟', '🎫']},
  {label: '科技', emojis: ['🤖', '👾', '🛸', '🚀', '🛰', '🛸', '🔭', '📡', '🧬']},
  {
    label: '交通',
    emojis: ['🚗', '🚕', '🚙', '🚌', '🚎', '🏎', '🚓', '🚑', '🚒', '🚐', '🚚', '🚛', '🚜', '🛴', '🚲', '🛵', '🏍', '🚄', '🚅', '🚈', '🚝', '🚂', '🚆', '🚇', '✈️', '🚁', '🛩', '⛵', '🚤', '🛥', '🛳', '🚢']
  },
  {
    label: '皇冠',
    emojis: ['👑', '💍', '👒', '🎩', '🎓', '🧢', '⛑', '🪖', '💄', '💋', '👓', '🕶', '🥽', '🥼', '🦺', '👔', '👕', '👖', '🧣', '🧤', '🧥', '🧦', '👗', '👘', '👙', '👚', '👜', '👛', '🎒', '👝', '🧳', '🌂', '☂️']
  },
  {
    label: '建筑',
    emojis: ['🏠', '🏡', '🏢', '🏣', '🏤', '🏥', '🏦', '🏨', '🏩', '🏪', '🏫', '🏬', '🏭', '🏯', '🏰', '💒', '🗼', '🗽', '⛪', '🕌', '🕍', '⛩', '🕋', '⛲', '🗿', '🏗', '🏘', '🏚']
  },
  {label: '星座', emojis: ['♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓', '⛎']},
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

/** 判断是否为 emoji */
function isEmoji(s: string): boolean {
  if (s.length > 2) return false
  const cp = s.codePointAt(0) ?? 0
  return (cp >= 0x1F300 && cp <= 0x1F9FF) || (cp >= 0x2600 && cp <= 0x27BF)
      || (cp >= 0xFE00 && cp <= 0xFE0F) || cp >= 0x200D
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
          <span class="preview-label">预览：</span>
          <span class="preview-char">{{ inputText || '?' }}</span>
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
        <div v-if="!selectedFile" class="upload-placeholder">
          <label class="upload-btn" @click="pickImage">选择图片</label>
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
  max-height: 320px;
  overflow-y: auto;
}

.emoji-group {
  margin-bottom: 10px;
}

.emoji-group-label {
  font-size: 11px;
  color: #9ca3af;
  padding: 2px 0 4px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 3px;
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
