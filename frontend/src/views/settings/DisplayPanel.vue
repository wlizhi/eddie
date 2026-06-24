<template>
  <div class="panel">
    <h3 class="panel-title">显示设置</h3>

    <div class="settings-section">
      <!-- 字体大小 -->
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
            <input
                type="number"
                class="font-size-input"
                v-model.number="fontSizeInput"
                placeholder="建议12~24"
                min="10"
                max="28"
                @input="handleFontSizeInput"
                @blur="handleFontSizeBlur"
            />
            <span class="font-size-unit">px</span>
          </div>
        </div>
      </div>

      <!-- 字体类型 -->
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

      <!-- 外观（亮色/深色） -->
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

      <!-- 主题 -->
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

      <!-- 强调色 -->
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
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

const themeList = computed(() => getThemes())

const fontOptions = FONT_OPTIONS

const fontSizeLevels: { value: FontSizeLevel; label: string }[] = [
  {value: 'small', label: '小'},
  {value: 'medium', label: '中'},
  {value: 'large', label: '大'},
]

/** 输入框中显示的像素值 */
const fontSizeInput = ref<number>(getEffectiveFontSize())

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
