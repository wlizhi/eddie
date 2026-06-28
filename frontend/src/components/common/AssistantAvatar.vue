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
      (cp === 0x200D) ||
      (cp >= 0x1FA00 && cp <= 0x1FA6F) ||
      (cp >= 0x1F600 && cp <= 0x1F64F) ||
      (cp >= 0x2702 && cp <= 0x27B0)
  )
}

/** 判断是否为单字符（非 emoji 的普通文字） */
function isSingleChar(s: string | null | undefined): boolean {
  if (!s) return false
  return [...s].length === 1
}

// ========== 计算显示内容 ==========

/** 头像类型：image / emoji / char（文字或首字统一处理） */
const avatarType = computed<'image' | 'emoji' | 'char'>(() => {
  if (!props.avatar) return 'char'
  if (props.avatar.startsWith('http://') || props.avatar.startsWith('https://') || props.avatar.startsWith('/') || props.avatar.startsWith('blob:') || props.avatar.startsWith('data:')) {
    return 'image'
  }
  if (isEmojiChar(props.avatar)) return 'emoji'
  return 'char'
})

const emojiChar = computed(() =>
    avatarType.value === 'emoji' ? props.avatar! : null
)

/** 要显示的文字：用户设置了文字头像就用该文字，否则用名称首字 */
const displayChar = computed(() => {
  if (avatarType.value === 'char' && isSingleChar(props.avatar!)) return props.avatar!
  return props.name.charAt(0)
})

/** emoji 用更大的字号，因 emoji 在相同 font-size 下视觉偏小 */
const emojiFontSize = computed(() =>
    (props.size * 0.8) + 'px'
)

// ========== 颜色 ==========

const colors = [
  '#2563eb', '#059669', '#d97706', '#dc2626', '#7c3aed',
  '#0891b2', '#be185d', '#65a30d', '#c026d3', '#ea580c',
]

function hashColor(s: string): string {
  let hash = 0
  for (let i = 0; i < s.length; i++) {
    hash = s.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

/** 文字/首字统一：用显示字符的哈希值决定背景色 */
const bgColor = computed(() => {
  if (avatarType.value === 'char') return hashColor(displayChar.value)
  return 'transparent'
})

/** 统一使用白色文字 */
const textColor = computed(() => '#ffffff')
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
    <span v-else class="avatar-char">{{ displayChar }}</span>
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

.avatar-char {
  line-height: 1;
}
</style>
