<!--
 * @author Eddie
 * @date 2026-06-22
-->

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
        <div class="profile-avatar-wrap" @click="pickerInitialAvatar = displaySettings.avatar ?? null; showAvatarPicker = true"
             title="点击修改头像">
          <AssistantAvatar
              :name="displaySettings.nickname || '我'"
              :avatar="displaySettings.avatar"
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
            @blur="handleNicknameBlur"
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
        <n-select
            v-model:value="displaySettings.fontFamily"
            :options="fontOptions"
            :consistent-menu-width="false"
            menu-height="22rem"
            class="font-family-select"
            placeholder="选择字体"
        />
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
          <span class="setting-hint">配色风格</span>
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
              v-for="scheme in COLOR_SCHEMES"
              :key="scheme.color"
              class="color-swatch"
              :class="{ active: displaySettings.colorScheme === scheme.color }"
              :title="scheme.label"
              @click="displaySettings.colorScheme = scheme.color"
              :style="{ background: scheme.color }"
          >
          </button>
          <div class="color-picker-wrap"
               :class="{ active: !isPresetColor(displaySettings.colorScheme) }"
               :style="{ background: !isPresetColor(displaySettings.colorScheme) ? displaySettings.colorScheme : '' }">
            <input
                type="color"
                :value="displaySettings.colorScheme"
                @input="onColorPickerInput"
                class="color-picker-input"
                title="自定义颜色"
            />
            <span class="color-picker-icon"
                  :class="{ 'is-custom': !isPresetColor(displaySettings.colorScheme) }">🎨</span>
          </div>
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


      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">工具响应最大渲染长度</span>
          <span class="setting-hint">工具调用响应的最大渲染字符数限制（100~8000）</span>
        </div>
        <n-input-number
            v-model:value="toolCallMaxLength"
            :min="100"
            :max="8000"
            :step="100"
            :show-button="false"
            class="number-input"
            placeholder="5000"
        />
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

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">模型名称</span>
          <span class="setting-hint">在助手昵称旁显示模型名称</span>
        </div>
        <NSwitch v-model:value="displaySettings.showMetaModel"/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {NInputNumber, NModal, NSelect, NSwitch} from 'naive-ui'
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
  isPresetColor,
  loadDisplaySettings,
  MAX_RECOMMENDED,
  MIN_RECOMMENDED,
  saveDisplaySettings,
} from '@/composables/useDisplaySettings'
import {fetchConfigs, updateConfigs, updateUserAvatar} from '@/api/settings'
import AssistantAvatar from '@/components/common/AssistantAvatar.vue'
import AvatarPicker from '@/components/common/AvatarPicker.vue'

const themeList = computed(() => getThemes())

import type {SelectMixedOption} from 'naive-ui/es/select/src/interface'

const fontOptions = FONT_OPTIONS as SelectMixedOption[]

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

// ===== 工具响应最大渲染长度 =====
const toolCallMaxLength = ref(5000)

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
  if (val === undefined || val === null || isNaN(val as number)) return
  const num = Number(val)
  displaySettings.customFontSize = num
  // 检查是否匹配某个预设，匹配则同步高亮
  const matchLevel = (Object.entries(FONT_SIZE_MAP) as [FontSizeLevel, number][])
      .find(([, px]) => px === num)
  if (matchLevel) {
    displaySettings.fontSize = matchLevel[0]
  }
}

/** 输入框失焦时做 clamp 兜底并保存 */
function handleFontSizeBlur() {
  const val = fontSizeInput.value
  if (val === undefined || val === null || isNaN(val as number)) {
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
  applyDisplay()
  saveDisplaySettings().catch(() => {
  })
}

/** 调色盘实时输入处理 */
function onColorPickerInput(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.value) {
    displaySettings.colorScheme = target.value
  }
}

/** 昵称输入框失焦时保存 */
function handleNicknameBlur() {
  saveDisplaySettings().catch(() => {
  })
}

onMounted(async () => {
  await loadDisplaySettings()
  // 加载完成后同步输入框显示值
  fontSizeInput.value = getEffectiveFontSize()

  // 加载工具响应最大渲染长度配置
  try {
    const configs = await fetchConfigs()
    if (configs && configs.TOOL_CALL_RENDER_MAX_LENGTH) {
      toolCallMaxLength.value = parseInt(configs.TOOL_CALL_RENDER_MAX_LENGTH, 10)
    }
  } catch {
    // 加载失败使用默认值
  }
})

// 主题/外观变更 → 完整应用主题
watch([() => displaySettings.themeId, () => displaySettings.themeMode], async () => {
    applyDisplay()
    try {
        await saveDisplaySettings()
    } catch {
        // 保存失败静默处理
    }
})

// 强调色变更 → 仅更新强调色变量
watch(() => displaySettings.colorScheme, async () => {
    applyDisplay()
    try {
        await saveDisplaySettings()
    } catch {
        // 保存失败静默处理
    }
})

// 字体、文本类设置 → 仅更新字体变量
watch([() => displaySettings.fontFamily, () => displaySettings.fontSize], async () => {
    applyDisplay()
    try {
        await saveDisplaySettings()
    } catch {
        // 保存失败静默处理
    }
})

// 其余非主题设置（wideMode、chatMode、元数据开关、头像）→ 仅保存，不触发任何 CSS 变量重设
watch([
    () => displaySettings.wideMode,
    () => displaySettings.chatMode,
    () => displaySettings.showMetaTime,
    () => displaySettings.showMetaDuration,
    () => displaySettings.showMetaTokens,
    () => displaySettings.showMetaCost,
    () => displaySettings.showMetaModel,
    () => displaySettings.avatar,
], async () => {
    try {
        await saveDisplaySettings()
    } catch {
        // 保存失败静默处理
    }
})

/** 监听工具响应最大渲染长度变化，自动保存 */
watch(
    toolCallMaxLength,
    async (newValue) => {
      if (newValue === undefined || newValue === null) return
      try {
        await updateConfigs({
          TOOL_CALL_RENDER_MAX_LENGTH: String(newValue)
        })
      } catch {
        // 保存失败静默处理
      }
    }
)
</script>

<style src="./displayPanel.css" scoped/>
