<script setup lang="ts">
const props = withDefaults(defineProps<{
  name: string
  avatar?: string | null // null=首字, emoji like "🤖", 或图片路径（未来）
  size?: number
}>(), {
  avatar: null,
  size: 28,
})

// 根据名字生成固定颜色
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

const bgColor = hashColor(props.name)
const initial = props.name.charAt(0)
const isEmoji = props.avatar && props.avatar.startsWith('emoji:')
const emojiChar = isEmoji ? props.avatar!.replace('emoji:', '') : null
</script>

<template>
  <div
      class="avatar"
      :style="{
      width: size + 'px',
      height: size + 'px',
      fontSize: (size * 0.45) + 'px',
      background: avatar && !isEmoji ? 'transparent' : bgColor,
    }"
  >
    <!-- 图片头像（未来使用） -->
    <img v-if="avatar && !isEmoji" :src="avatar" :alt="name" class="avatar-img"/>
    <!-- Emoji 头像 -->
    <span v-else-if="emojiChar" class="avatar-emoji">{{ emojiChar }}</span>
    <!-- 首字头像 -->
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
