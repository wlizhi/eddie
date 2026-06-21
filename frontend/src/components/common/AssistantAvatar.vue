<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{
  name: string
  avatar?: string | null // null/空=首字, emoji字符, 文字字符, 或图片URL
  size?: number
}>(), {
  avatar: null,
  size: 28,
})

/** 判断是否为 emoji 字符 */
function isEmojiChar(s: string): boolean {
  if (s.length > 2) return false
  const cp = s.codePointAt(0) ?? 0
  return (
      (cp >= 0x1F300 && cp <= 0x1F9FF) ||
      (cp >= 0x2600 && cp <= 0x27BF) ||
      (cp >= 0xFE00 && cp <= 0xFE0F) ||
      (cp >= 0x200D) ||
      (cp >= 0x1FA00 && cp <= 0x1FA6F) ||
      (cp >= 0x1F600 && cp <= 0x1F64F) ||
      (cp >= 0x2702 && cp <= 0x27B0)
  )
}

/** 判断是否为单字符（非 emoji 的普通文字） */
function isSingleChar(s: string): boolean {
  return [...s].length === 1
}

// ========== 计算头像类型 ==========

const avatarType = computed<'image' | 'emoji' | 'text' | 'initial'>(() => {
  if (!props.avatar) return 'initial'
  if (props.avatar.startsWith('http://') || props.avatar.startsWith('https://') || props.avatar.startsWith('/') || props.avatar.startsWith('blob:') || props.avatar.startsWith('data:')) {
    return 'image'
  }
  if (isEmojiChar(props.avatar)) return 'emoji'
  if (isSingleChar(props.avatar)) return 'text'
  return 'initial'
})

const emojiChar = computed(() =>
    avatarType.value === 'emoji' ? props.avatar! : null
)

const textChar = computed(() =>
    avatarType.value === 'text' ? props.avatar! : null
)

/** emoji 用更大的字号，因 emoji 在相同 font-size 下视觉偏小 */
const emojiFontSize = computed(() =>
    (props.size * 0.8) + 'px'
)

const initial = computed(() => props.name.charAt(0))

// ========== 背景颜色 ==========

const colors = [
  '#2563eb', '#059669', '#d97706', '#dc2626', '#7c3aed',
  '#0891b2', '#be185d', '#65a30d', '#c026d3', '#ea580c',
]

function hashColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

/** 文字头像浅色背景 */
const textColors = ['#e0e7ff', '#d1fae5', '#fef3c7', '#fee2e2', '#ede9fe']

function hashTextColor(s: string): string {
  let hash = 0
  for (let i = 0; i < s.length; i++) {
    hash = s.charCodeAt(i) + ((hash << 5) - hash)
  }
  return textColors[Math.abs(hash) % textColors.length]
}

const bgColor = computed(() => {
  if (avatarType.value === 'initial') return hashColor(props.name)
  if (avatarType.value === 'text') return hashTextColor(props.avatar!)
  return 'transparent'
})

const textColor = computed(() => {
  if (avatarType.value === 'text') return '#374151'
  return '#ffffff'
})
</script>

<template>
  <div
      class="avatar"
      :style="{
      width: size + 'px',
      height: size + 'px',
      fontSize: (size * 0.45) + 'px',
      background: bgColor,
      color: textColor,
    }"
  >
    <img v-if="avatarType === 'image'" :src="avatar!" :alt="name" class="avatar-img"/>
    <span v-else-if="avatarType === 'emoji'" class="avatar-emoji" :style="{ fontSize: emojiFontSize }">{{
        emojiChar
      }}</span>
    <span v-else-if="avatarType === 'text'" class="avatar-text">{{ textChar }}</span>
    <span v-else class="avatar-initial">{{ initial }}</span>
  </div>
</template>

<style scoped>
.avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  font-weight: 600;
  line-height: 1;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-emoji {
  font-size: inherit;
  line-height: 1;
}

.avatar-text {
  line-height: 1;
}

.avatar-initial {
  line-height: 1;
}
</style>
