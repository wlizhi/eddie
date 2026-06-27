<template>
  <div class="panel">
    <!-- ===== 个人信息 ===== -->
    <div class="settings-group">
      <div class="group-label">个人信息</div>

      <div class="setting-row profile-row">
        <div class="setting-info">
          <span class="setting-label">头像</span>
          <span class="setting-hint">文字、Emoji 或上传图片</span>
        </div>
        <div class="profile-avatar-wrap" @click="showAvatarPicker = true" title="点击修改头像">
          <AssistantAvatar
              :name="displaySettings.nickname || '我'"
              :avatar="displaySettings.avatar || null"
              :size="56"
          />
          <div class="avatar-overlay">编辑</div>
        </div>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">昵称</span>
          <span class="setting-hint">在所有聊天页面中显示</span>
        </div>
        <input
            v-model="displaySettings.nickname"
            class="profile-nickname-input"
            placeholder="输入昵称"
            maxlength="20"
        />
      </div>
    </div>

    <!-- 头像选择弹窗 -->
    <NModal :show="showAvatarPicker" preset="card" title="选择头像"
            style="max-width: 420px; width: 90%;"
            :mask-closable="false"
            @update:show="(v: boolean) => { if (!v) showAvatarPicker = false }">
      <AvatarPicker :current-avatar="pickerInitialAvatar"
                    @confirm="onAvatarPicked"
                    @close="showAvatarPicker = false"/>
    </NModal>

    <!-- ===== 字体 ===== -->
    <div class="settings-group">
      <div class="group-label">字体</div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">字体大小</span>
          <span class="setting-hint">调整页面全局字体尺寸（建议值：{{ MIN_RECOMMENDED }}-{{ MAX_RECOMMENDED }}px）</span>
        </div>
        <div class="font-size-controls">
          <div class="size-selector">
            <button
                v-for="level in fontSizeLevels"
                :key="level.value"
                class="size-option"
                :class="{ active: displaySettings.fontSize === level.value }"
                @click="handleFontSizeClick(level.value)"
            >
              {{ level.label }}
            </button>
          </div>
          <div class="font-size-input-group">
            <n-input-number
                v-model:value="fontSizeInput"
                class="font-size-input"
                placeholder="建议12~24"
                :min="10"
                :max="28"
                :step="1"
                :show-button="false"
                @update:value="handleFontSizeInput"
                @blur="handleFontSizeBlur"
            />
            <span class="font-size-unit">px</span>
          </div>
        </div>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">字体类型</span>
          <span class="setting-hint">选择页面使用的字体</span>
        </div>
        <select v-model="displaySettings.fontFamily" class="font-select">
          <option
              v-for="opt in fontOptions"
              :key="opt.value"
              :value="opt.value"
          >{{ opt.label }}
          </option>
        </select>
      </div>
    </div>

    <!-- ===== 主题 ===== -->
    <div class="settings-group">
      <div class="group-label">主题</div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">外观</span>
          <span class="setting-hint">选择浅色或深色模式</span>
        </div>
        <div class="size-selector">
          <button
              class="size-option"
              :class="{ active: displaySettings.themeMode === 'light' }"
              @click="displaySettings.themeMode = 'light'"
          >☀️ 浅色
          </button>
          <button
              class="size-option"
              :class="{ active: displaySettings.themeMode === 'dark' }"
              @click="displaySettings.themeMode = 'dark'"
          >🌙 深色
          </button>
        </div>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">主题</span>
          <span class="setting-hint">在当前外观下选择配色风格</span>
        </div>
        <div class="theme-selector">
          <button
              v-for="t in themeList"
              :key="t.id"
              class="theme-card"
              :class="{ active: displaySettings.themeId === t.id }"
              @click="displaySettings.themeId = t.id"
          >
            <div class="theme-preview">
              <span class="theme-swatch" :style="{ background: t.color }"></span>
              <span class="theme-swatch dark" :style="{ background: t.darkColor }"></span>
            </div>
            <span class="theme-name">{{ t.name }}</span>
          </button>
        </div>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">强调色</span>
          <span class="setting-hint">选中项、按钮、链接等统一色调</span>
        </div>
        <div class="color-scheme-selector">
          <button
              v-for="(scheme, key) in COLOR_SCHEMES"
              :key="key"
              class="color-swatch"
              :class="{ active: displaySettings.colorScheme === key }"
              :title="scheme.label"
              @click="displaySettings.colorScheme = key"
              :style="{ background: scheme.color }"
          >
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 聊天 ===== -->
    <div class="settings-group">
      <div class="group-label">聊天</div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">宽屏模式</span>
          <span class="setting-hint">开启后消息区域占满可用宽度</span>
        </div>
        <NSwitch v-model:value="displaySettings.wideMode"/>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">消息展示</span>
          <span class="setting-hint">聊天模式用户消息右对齐，问答模式统一左对齐</span>
        </div>
        <div class="size-selector">
          <button
              class="size-option"
              :class="{ active: displaySettings.chatMode }"
              @click="displaySettings.chatMode = true"
          >💬 聊天
          </button>
          <button
              class="size-option"
              :class="{ active: !displaySettings.chatMode }"
              @click="displaySettings.chatMode = false"
          >📄 问答
          </button>
        </div>
      </div>

      <div class="group-label" style="margin-top: 8px;">消息元数据</div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">时间</span>
          <span class="setting-hint">显示消息发送时间</span>
        </div>
        <NSwitch v-model:value="displaySettings.showMetaTime"/>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">接口耗时</span>
          <span class="setting-hint">显示每条消息的响应耗时</span>
        </div>
        <NSwitch v-model:value="displaySettings.showMetaDuration"/>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">Token 用量</span>
          <span class="setting-hint">显示输入/输出 token 数量和缓存命中</span>
        </div>
        <NSwitch v-model:value="displaySettings.showMetaTokens"/>
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">花费估算</span>
          <span class="setting-hint">显示每次对话的预估费用</span>
        </div>
        <NSwitch v-model:value="displaySettings.showMetaCost"/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {NInputNumber, NModal, NSwitch} from 'naive-ui'
import {
  applyDisplay,
  clampFontSize,
  COLOR_SCHEMES,
  displaySettings,
  FONT_OPTIONS,
  FONT_SIZE_MAP,
  type FontSizeLevel,
  getEffectiveFontSize,
  getThemes,
  loadDisplaySettings,
  MAX_RECOMMENDED,
  MIN_RECOMMENDED,
  saveDisplaySettings,
} from '@/composables/useDisplaySettings'
import {updateUserAvatar} from '@/api/settings'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'
import AvatarPicker from '@/components/common/AvatarPicker.vue'

const themeList = computed(() => getThemes())

const fontOptions = FONT_OPTIONS

const fontSizeLevels: { value: FontSizeLevel; label: string }[] = [
  {value: 'small', label: '小'},
  {value: 'medium', label: '中'},
  {value: 'large', label: '大'},
]

/** 输入框中显示的像素值 */
const fontSizeInput = ref<number>(getEffectiveFontSize())

// ===== 个人信息 - 头像 =====
const showAvatarPicker = ref(false)
const pickerInitialAvatar = ref<string | null>(null)

function onAvatarPicked(value: string | null, file: File | null) {
  showAvatarPicker.value = false
  if (!value && !file) return

  // 图片上传 → 调用后端接口保存
  if (file) {
    updateUserAvatar(undefined, file).then((url) => {
      displaySettings.avatar = url
    }).catch(() => {
      // 上传失败静默处理
    })
    return
  }

  // 文字/emoji → 直接存值
  if (value) {
    updateUserAvatar(value, undefined).then((url) => {
      displaySettings.avatar = url
    }).catch(() => {
      // 也支持本地直接更新（不回退）
      displaySettings.avatar = value
    })
  }
}

/** 点击预设按钮 */
function handleFontSizeClick(level: FontSizeLevel) {
  displaySettings.fontSize = level
  // 清除自定义值，回退到预设
  displaySettings.customFontSize = undefined
  fontSizeInput.value = FONT_SIZE_MAP[level]
}

/** 手动输入字体大小（仅更新，不做校验，避免干扰输入） */
function handleFontSizeInput() {
  const val = fontSizeInput.value
  if (val === undefined || val === null || val === '' || isNaN(val as number)) return
  const num = Number(val)
  displaySettings.customFontSize = num
  // 检查是否匹配某个预设，匹配则同步高亮
  const matchLevel = (Object.entries(FONT_SIZE_MAP) as [FontSizeLevel, number][])
      .find(([, px]) => px === num)
  if (matchLevel) {
    displaySettings.fontSize = matchLevel[0]
  }
}

/** 输入框失焦时做 clamp 兜底 */
function handleFontSizeBlur() {
  const val = fontSizeInput.value
  if (val === undefined || val === null || val === '' || isNaN(val as number)) {
    // 输入为空或无效时，回退到当前生效值
    fontSizeInput.value = getEffectiveFontSize()
    return
  }
  const clamped = clampFontSize(Number(val))
  if (clamped !== Number(val)) {
    fontSizeInput.value = clamped
  }
  displaySettings.customFontSize = clamped
  // 检查是否匹配预设
  const matchLevel = (Object.entries(FONT_SIZE_MAP) as [FontSizeLevel, number][])
      .find(([, px]) => px === clamped)
  if (matchLevel) {
    displaySettings.fontSize = matchLevel[0]
  }
}

onMounted(async () => {
  await loadDisplaySettings()
  // 加载完成后同步输入框显示值
  fontSizeInput.value = getEffectiveFontSize()
})

/** 监听设置变化，即时生效并自动保存 */
watch(
    () => ({...displaySettings}),
    async () => {
      applyDisplay()
      try {
        await saveDisplaySettings()
      } catch {
        // 保存失败静默处理
      }
    },
    {deep: true},
)
</script>

<style src="./displayPanel.css" scoped/>
