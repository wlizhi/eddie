<!--
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手配置面板
 * 配置存储在 SELECTION_ASSISTANT_CONFIG 全局配置 key 中
-->

<template>
  <div class="panel">
    <!-- ===== 助手 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <Bot :size="16" :stroke-width="2" class="group-icon"/>
        助手
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">启用</span>
          <span class="setting-hint">开启后选中文本时自动弹出工具栏</span>
        </div>
        <n-switch
            :value="config.enabled"
            @update:value="onEnabledChange"
        />
      </div>
    </div>

    <!-- ===== 工具栏 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <PanelTop :size="16" :stroke-width="2" class="group-icon"/>
        工具栏
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">显示风格</span>
          <span class="setting-hint">紧凑风格仅显示图标，不显示文字</span>
        </div>
        <div class="size-selector">
          <button
              class="size-option"
              :class="{ active: config.toolbar.style === 'default' }"
              @click="config.toolbar.style = 'default'; saveConfig()"
          >默认
          </button>
          <button
              class="size-option"
              :class="{ active: config.toolbar.style === 'compact' }"
              @click="config.toolbar.style = 'compact'; saveConfig()"
          >紧凑
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 功能窗口 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <PanelRight :size="16" :stroke-width="2" class="group-icon"/>
        功能窗口
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">记住大小</span>
          <span class="setting-hint">开启后窗口调整大小后，下次打开保持相同尺寸</span>
        </div>
        <n-switch
            :value="config.window.rememberSize"
            @update:value="onWindowFieldChange('rememberSize', $event)"
        />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">自动关闭</span>
          <span class="setting-hint">开启后点击窗口外区域时窗口自动关闭</span>
        </div>
        <n-switch
            :value="config.window.autoClose"
            @update:value="onWindowFieldChange('autoClose', $event)"
        />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">默认置顶</span>
          <span class="setting-hint">开启后弹窗默认置顶显示</span>
        </div>
        <n-switch
            :value="config.window.alwaysOnTop"
            @update:value="onWindowFieldChange('alwaysOnTop', $event)"
        />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">透明度</span>
          <span class="setting-hint">控制窗口的整体透明度（0=完全透明，100=完全不透明）</span>
        </div>
        <div class="opacity-control">
          <n-slider
              :value="config.window.opacity"
              :min="0"
              :max="100"
              :step="1"
              style="width: 120px"
              @update:value="onOpacityChange"
          />
          <span class="opacity-value">{{ config.window.opacity }}%</span>
        </div>
      </div>
    </div>

    <!-- ===== 功能 ===== -->
    <div class="settings-group">
      <div class="group-label">
        <List :size="16" :stroke-width="2" class="group-icon"/>
        功能
      </div>

      <div class="group-hint setting-hint">拖拽排序，禁用后工具栏中不显示该按钮</div>

      <div
          ref="featureListRef"
          class="feature-list"
      >
        <div
            v-for="(item, index) in config.features"
            :key="item.id"
            class="feature-row"
            :class="{ 'drag-over': dragOverIndex === index }"
            draggable="true"
            @dragstart="onDragStart(index)"
            @dragover="onDragOver($event, index)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop"
            @dragend="onDragEnd"
        >
          <span class="drag-handle"><GripVertical :size="14" :stroke-width="1.5"/></span>
          <span class="feature-icon" v-html="item.icon"></span>
          <span class="feature-label">{{ item.label }}</span>
          <n-switch
              :size="'small'"
              :value="item.enabled"
              @update:value="onFeatureToggle(item.id, $event)"
          />
        </div>

        <!-- 末尾拖入区：拖动到列表末尾时显示 -->
        <div
            v-if="dragIndex !== null"
            class="feature-drop-end"
            :class="{ 'drag-over': dragOverIndex === config.features.length }"
            @dragover.prevent="onDropEndOver"
            @dragleave="onDropEndLeave"
            @drop.prevent="onDropEnd"
        >
          <span class="drop-end-hint">拖到此处放入末尾</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {NSlider, NSwitch} from 'naive-ui'
import {Bot, GripVertical, List, PanelRight, PanelTop} from '@lucide/vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {showToast} from '@/composables/useToast'
import {useDragSort} from '@/composables/useDragSort'

const CONFIG_KEY = 'SELECTION_ASSISTANT_CONFIG'

// ===== SVG 图标（与 electron 配置保持一致） =====
const SVG_GLOBE = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M2.5 5h9"/><path d="M2.5 9h9"/><path d="M7 1.5a7 7 0 0 1 0 11"/><path d="M7 1.5a7 7 0 0 0 0 11"/></svg>'
const SVG_BOOK = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M2 2.5v9a1 1 0 0 0 1 1h3.5L7 11l.5 1.5H11a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1z"/><path d="M7 11V4"/></svg>'
const SVG_LIST = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="3.5" x2="11.5" y2="3.5"/><line x1="5" y1="7" x2="11.5" y2="7"/><line x1="5" y1="10.5" x2="11.5" y2="10.5"/><circle cx="2.5" cy="3.5" r=".8"/><circle cx="2.5" cy="7" r=".8"/><circle cx="2.5" cy="10.5" r=".8"/></svg>'
const SVG_COPY = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><rect x="4.5" y="4.5" width="7" height="7" rx=".8"/><path d="M2.5 10.5v-7a1 1 0 0 1 1-1h7"/></svg>'
const SVG_BEAUTIFY = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.64 3.64-1.28-1.28a1.21 1.21 0 0 0-1.72 0L2.36 18.64a1.21 1.21 0 0 0 0 1.72l1.28 1.28a1.2 1.2 0 0 0 1.72 0L21.64 5.36a1.2 1.2 0 0 0 0-1.72"/><path d="m14 7 3 3"/><path d="M5 6v4"/><path d="M19 14v4"/><path d="M10 2v2"/><path d="M7 8H3"/><path d="M21 16h-4"/><path d="M11 3H9"/></svg>'

interface FeatureItem {
  id: string
  label: string
  icon: string
  enabled: boolean
  order: number
}

interface SelectionAssistantConfig {
  enabled: boolean
  toolbar: {
    style: 'default' | 'compact'
  }
  window: {
    rememberSize: boolean
    autoClose: boolean
    alwaysOnTop: boolean
    opacity: number
  }
  features: FeatureItem[]
}

const DEFAULT_CONFIG: SelectionAssistantConfig = {
  enabled: false,
  toolbar: {
    style: 'default',
  },
  window: {
    rememberSize: false,
    autoClose: false,
    alwaysOnTop: false,
    opacity: 100,
  },
  features: [
    {id: 'translate', label: '翻译', icon: SVG_GLOBE, enabled: true, order: 1},
    {id: 'explain', label: '解释', icon: SVG_BOOK, enabled: true, order: 2},
    {id: 'summarize', label: '总结', icon: SVG_LIST, enabled: true, order: 3},
    {id: 'copy', label: '复制', icon: SVG_COPY, enabled: true, order: 4},
    {id: 'beautify', label: '美化', icon: SVG_BEAUTIFY, enabled: true, order: 5},
  ],
}

const config = reactive<SelectionAssistantConfig>({...DEFAULT_CONFIG})

const featureListRef = ref<HTMLElement | null>(null)

// 拖拽排序 — 使用 useDragSort 的状态管理，但覆写排序逻辑修正向下拖拽的索引偏移
const {
  dragIndex: dsDragIndex, dragOverIndex: dsDragOverIndex,
  onDragStart, onDragOver: dsOnDragOver, onDragLeave, onDragEnd,
} = useDragSort(
    () => config.features,
    async () => {/* 排序由本地 handleDrop 完成 */},
)

// 暴露 refs 供模板使用
const dragIndex = dsDragIndex
const dragOverIndex = dsDragOverIndex

/** 修正后的 drop 处理：向下拖拽时目标索引 -1 */
function handleDrop() {
  if (dragIndex.value === null || dragOverIndex.value === null) return
  if (dragIndex.value === dragOverIndex.value) {
    dragIndex.value = null
    dragOverIndex.value = null
    return
  }

  const items = [...config.features]
  const [moved] = items.splice(dragIndex.value, 1)
  // 关键修正：如果目标位置在源位置之后，删除源项后目标位置前移一位
  const insertAt = dragOverIndex.value > dragIndex.value
      ? dragOverIndex.value - 1
      : dragOverIndex.value
  items.splice(insertAt, 0, moved)

  // 重算 order 并写回
  const reordered = items.map((item, idx) => ({...item, order: idx + 1}))
  config.features.length = 0
  config.features.push(...reordered)

  dragIndex.value = null
  dragOverIndex.value = null
  saveConfig()
}

/** 覆写 onDragOver：使用 useDragSort 的标准实现 */
function onDragOver(e: DragEvent, index: number) {
  dsOnDragOver(e, index)
}

/** 覆写 onDrop：使用修正后的实现 */
function onDrop() {
  handleDrop()
}

/** 拖到末尾区域 */
function onDropEndOver() {
  if (dragIndex.value === null) return
  dragOverIndex.value = config.features.length
}
function onDropEndLeave() {
  if (dragOverIndex.value === config.features.length) {
    dragOverIndex.value = null
  }
}
function onDropEnd() {
  if (dragIndex.value === null) return
  // 拖到末尾：insertAt = features.length（无需修正，因为一定在源位置之后）
  const items = [...config.features]
  const [moved] = items.splice(dragIndex.value, 1)
  items.splice(config.features.length - 1, 0, moved)

  const reordered = items.map((item, idx) => ({...item, order: idx + 1}))
  config.features.length = 0
  config.features.push(...reordered)

  dragIndex.value = null
  dragOverIndex.value = null
  saveConfig()
}

async function loadConfig() {
  try {
    const configs = await fetchConfigs()
    const raw = configs[CONFIG_KEY]
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<SelectionAssistantConfig>
      if (parsed.enabled != null) config.enabled = parsed.enabled
      if (parsed.toolbar) {
        if (parsed.toolbar.style) config.toolbar.style = parsed.toolbar.style
      }
      if (parsed.window) {
        if (parsed.window.rememberSize != null) config.window.rememberSize = parsed.window.rememberSize
        if (parsed.window.autoClose != null) config.window.autoClose = parsed.window.autoClose
        if (parsed.window.alwaysOnTop != null) config.window.alwaysOnTop = parsed.window.alwaysOnTop
        if (parsed.window.opacity != null) config.window.opacity = parsed.window.opacity
      }
      if (parsed.features && parsed.features.length > 0) {
        // 合并策略：以默认 features 为基底，用已保存的覆盖 enabled/order，保留新增项（如 beautify）
        const savedFeatures = parsed.features
        const merged = DEFAULT_CONFIG.features.map(defaultF => {
          const saved = savedFeatures.find((f: FeatureItem) => f.id === defaultF.id)
          return saved ? {...defaultF, enabled: saved.enabled, order: saved.order} : defaultF
        })
        config.features.length = 0
        config.features.push(...merged)
      }
    }
  } catch (err: any) {
    showToast('加载划词助手配置失败: ' + (err.message || '未知错误'), 'error')
  }
}

async function saveConfig() {
  try {
    // 保存时忽略 icon 字段（由默认配置提供，保持存储最小化）
    const payload = {
      ...config,
      features: config.features.map(({icon, ...rest}) => rest),
    }
    await updateConfigs({[CONFIG_KEY]: JSON.stringify(payload)})
    // 通知 Electron 主进程重新从后端拉取配置（网页环境安全跳过）
    ;(window as any).electronAPI?.selectionConfigChanged()
  } catch (err: any) {
    showToast('保存划词助手配置失败: ' + (err.message || '未知错误'), 'error')
  }
}

function onEnabledChange(val: boolean) {
  config.enabled = val
  saveConfig()
}

function onWindowFieldChange(field: string, val: boolean) {
  ;(config.window as any)[field] = val
  saveConfig()
}

function onOpacityChange(val: number) {
  config.window.opacity = val
  saveConfig()
}

function onFeatureToggle(id: string, val: boolean) {
  const item = config.features.find(f => f.id === id)
  if (item) {
    item.enabled = val
    saveConfig()
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.panel {
  max-width: 30rem;
}

.settings-group {
  margin-bottom: 24px;
}

.group-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-lighter);
}

.group-icon {
  color: var(--text-tertiary);
}

.group-hint {
  margin-top: -12px;
  margin-bottom: 12px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.setting-info {
  flex: 1;
  min-width: 0;
}

.setting-label {
  display: block;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.setting-hint {
  display: block;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
  line-height: 1.4;
}

.size-selector {
  display: flex;
  gap: 4px;
}

.size-option {
  padding: 4px 12px;
  border: 1px solid var(--border-lighter);
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-small);
  cursor: pointer;
  transition: all 0.15s ease;
}

.size-option.active {
  border-color: var(--accent-default);
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.opacity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.opacity-value {
  min-width: 36px;
  font-size: var(--font-size-small);
  color: var(--text-secondary);
  text-align: right;
}

/* ===== 功能列表（与助手列表一致的拖拽实现） ===== */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.feature-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: background 0.15s;
  touch-action: manipulation;
  border-top: 2px solid transparent;
}

@media (hover: hover) {
  .feature-row:hover {
    background: var(--bg-hover);
  }
}

.feature-row.drag-over {
  border-top: 2px solid var(--accent-default);
  border-radius: 0;
}

.drag-handle {
  display: flex;
  align-items: center;
  color: var(--text-quaternary);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
}

.feature-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  color: var(--text-tertiary);
}

@media (hover: hover) {
  .feature-row:hover .drag-handle {
    color: var(--text-secondary);
  }
}

.feature-label {
  flex: 1;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 末尾拖入区 ===== */
.feature-drop-end {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  margin-top: 2px;
  border: 1px dashed var(--border-lighter);
  border-radius: 6px;
  transition: border-color 0.15s, background 0.15s;
}

.feature-drop-end.drag-over {
  border-color: var(--accent-default);
  background: var(--accent-light-bg);
}

.drop-end-hint {
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}
</style>
