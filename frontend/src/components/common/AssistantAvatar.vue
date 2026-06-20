<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{
  name: string
  avatar?: string | null // null/空=首字, emoji字符, 或图片URL
  size?: number
}>(), {
  avatar: null,
  size: 28,
})

/** 判断是否为 emoji 字符 */
function isEmojiChar(s: string): boolean {
  // 单个字符且在 emoji 常用 Unicode 范围内
  if (s.length > 2) return false
  const cp = s.codePointAt(0) ?? 0
  return (
      (cp >= 0x1F300 && cp <= 0x1F9FF) ||  // 杂项符号与表情、补充符号与表情
      (cp >= 0x2600 && cp <= 0x27BF) ||    // 杂项符号、装饰符号
      (cp >= 0xFE00 && cp <= 0xFE0F) ||    // 变体选择符
      (cp >= 0x200D) ||                    // 零宽连字
      (cp >= 0x1FA00 && cp <= 0x1FA6F) ||  // 符号与象形文字扩展A
      (cp >= 0x1F600 && cp <= 0x1F64F) ||  // 表情符号
      (cp >= 0x2702 && cp <= 0x27B0)       // 装饰符号
  )
}

// ========== 计算头像类型 ==========

const avatarType = computed<'image' | 'emoji' | 'initial'>(() => {
  if (!props.avatar) return 'initial'
  if (props.avatar.startsWith('http://') || props.avatar.startsWith('https://') || props.avatar.startsWith('/')) {
    return 'image'
  }
  if (isEmojiChar(props.avatar)) return 'emoji'
  return 'initial'
})

const emojiChar = computed(() =>
    avatarType.value === 'emoji' ? props.avatar! : null
)

/** emoji 用更大的字号，因 emoji 在相同 font-size 下视觉偏小 */
const emojiFontSize = computed(() =>
    (props.size * 0.8) + 'px'
)

const initial = computed(() => props.name.charAt(0))

// ========== 背景颜色（首字头像用） ==========

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

const bgColor = computed(() =>
    avatarType.value === 'initial' ? hashColor(props.name) : 'transparent'
)
</script>

<template>
  <div
      class="avatar"
      :style="{
      width: size + 'px',
      height: size + 'px',
      fontSize: (size * 0.45) + 'px',
      background: bgColor,
    }"
  >
    <img v-if="avatarType === 'image'" :src="avatar!" :alt="name" class="avatar-img"/>
    <span v-else-if="avatarType === 'emoji'" class="avatar-emoji" :style="{ fontSize: emojiFontSize }">{{
        emojiChar
      }}</span>
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
  color: #ffffff;
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

.avatar-initial {
  line-height: 1;
}
</style>
