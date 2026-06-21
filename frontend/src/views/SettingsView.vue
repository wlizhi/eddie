<script setup lang="ts">
import {ref} from 'vue'
import {Clock, Cpu, Globe, Monitor, Network, Palette, Puzzle, Radio, Search, Settings, Zap} from '@lucide/vue'

interface NavSection {
  key: string
  label: string
  icon: any
}

interface NavGroup {
  label?: string
  items: NavSection[]
}

const navGroups: NavGroup[] = [
  {
    label: '模型配置',
    items: [
      {key: 'model-provider', label: '模型服务', icon: Cpu},
      {key: 'default-model', label: '默认模型', icon: Zap},
    ],
  },
  {
    label: '通用设置',
    items: [
      {key: 'general', label: '常规设置', icon: Settings},
      {key: 'display', label: '显示设置', icon: Monitor},
    ],
  },
  {
    label: '扩展功能',
    items: [
      {key: 'mcp', label: 'MCP 服务器', icon: Network},
      {key: 'skills', label: '技能', icon: Puzzle},
      {key: 'web-search', label: '网络搜索', icon: Globe},
      {key: 'channels', label: '频道', icon: Radio},
      {key: 'scheduled-tasks', label: '定时任务', icon: Clock},
    ],
  },
]

const activeKey = ref<string>('model-provider')
</script>

<template>
  <div class="settings-layout">
    <!-- 左侧导航 -->
    <nav class="settings-nav">
      <template v-for="(group, gi) in navGroups" :key="gi">
        <div v-if="group.label" class="nav-group-label">{{ group.label }}</div>
        <button
            v-for="item in group.items"
            :key="item.key"
            class="nav-btn"
            :class="{ active: activeKey === item.key }"
            @click="activeKey = item.key"
        >
          <component :is="item.icon" :size="16" :stroke-width="1.8" class="nav-btn-icon"/>
          <span>{{ item.label }}</span>
        </button>
        <div v-if="gi < navGroups.length - 1" class="nav-divider"/>
      </template>
    </nav>

    <!-- 右侧内容 -->
    <div class="settings-content">
      <!-- 模型服务 -->
      <div v-if="activeKey === 'model-provider'" class="panel">
        <h3 class="panel-title">模型服务</h3>
        <p class="panel-desc">管理模型服务商，启用/禁用、添加自定义兼容服务。</p>
        <div class="panel-placeholder">
          <Cpu :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>模型服务商管理功能待实现</span>
        </div>
      </div>

      <!-- 默认模型（三模型配置） -->
      <div v-if="activeKey === 'default-model'" class="panel">
        <h3 class="panel-title">默认模型</h3>
        <p class="panel-desc">配置三类预设模型，供助手、智能体、翻译等功能默认选用。</p>

        <div class="config-card">
          <div class="config-header">
            <Zap :size="18" :stroke-width="2"/>
            <span>默认模型</span>
          </div>
          <p class="config-hint">创建助手或智能体时，未指定模型则使用此模型。建议选择综合能力强的模型。</p>
          <div class="config-row">
            <span class="config-label">服务商</span>
            <span class="config-value placeholder">DeepSeek</span>
          </div>
          <div class="config-row">
            <span class="config-label">模型</span>
            <span class="config-value placeholder">deepseek-v4-pro</span>
          </div>
        </div>

        <div class="config-card">
          <div class="config-header">
            <Zap :size="18" :stroke-width="2" class="fast-icon"/>
            <span>快速模型</span>
          </div>
          <p class="config-hint">用于生成会话标题、中期记忆压缩、长期记忆摘要等轻量杂活。建议选择便宜快速的模型。</p>
          <div class="config-row">
            <span class="config-label">服务商</span>
            <span class="config-value placeholder">DeepSeek</span>
          </div>
          <div class="config-row">
            <span class="config-label">模型</span>
            <span class="config-value placeholder">deepseek-v4-flash</span>
          </div>
        </div>

        <div class="config-card">
          <div class="config-header">
            <Globe :size="18" :stroke-width="2" class="translate-icon"/>
            <span>翻译模型</span>
          </div>
          <p class="config-hint">翻译功能专用模型。可根据需要选择翻译能力强的模型。</p>
          <div class="config-row">
            <span class="config-label">服务商</span>
            <span class="config-value placeholder">未配置</span>
          </div>
          <div class="config-row">
            <span class="config-label">模型</span>
            <span class="config-value placeholder">—</span>
          </div>
        </div>
      </div>

      <!-- 常规设置 -->
      <div v-if="activeKey === 'general'" class="panel">
        <h3 class="panel-title">常规设置</h3>
        <p class="panel-desc">应用的通用行为和偏好配置。</p>
        <div class="panel-placeholder">
          <Settings :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>常规设置功能待实现</span>
        </div>
      </div>

      <!-- 显示设置 -->
      <div v-if="activeKey === 'display'" class="panel">
        <h3 class="panel-title">显示设置</h3>
        <p class="panel-desc">主题、字体、布局等显示偏好。</p>
        <div class="panel-placeholder">
          <Palette :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>显示设置功能待实现</span>
        </div>
      </div>

      <!-- MCP 服务器 -->
      <div v-if="activeKey === 'mcp'" class="panel">
        <h3 class="panel-title">MCP 服务器</h3>
        <p class="panel-desc">管理 MCP (Model Context Protocol) 服务器连接。</p>
        <div class="panel-placeholder">
          <Network :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>MCP 服务器管理功能待实现</span>
        </div>
      </div>

      <!-- 技能 -->
      <div v-if="activeKey === 'skills'" class="panel">
        <h3 class="panel-title">技能</h3>
        <p class="panel-desc">管理和配置助手可用的技能模块。</p>
        <div class="panel-placeholder">
          <Puzzle :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>技能管理功能待实现</span>
        </div>
      </div>

      <!-- 网络搜索 -->
      <div v-if="activeKey === 'web-search'" class="panel">
        <h3 class="panel-title">网络搜索</h3>
        <p class="panel-desc">配置搜索引擎和网络搜索参数。</p>
        <div class="panel-placeholder">
          <Search :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>网络搜索功能待实现</span>
        </div>
      </div>

      <!-- 频道 -->
      <div v-if="activeKey === 'channels'" class="panel">
        <h3 class="panel-title">频道</h3>
        <p class="panel-desc">接入外部消息频道（如 QQ 机器人、飞书、Discord 等）。</p>
        <div class="panel-placeholder">
          <Radio :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>频道接入功能待实现</span>
        </div>
      </div>

      <!-- 定时任务 -->
      <div v-if="activeKey === 'scheduled-tasks'" class="panel">
        <h3 class="panel-title">定时任务</h3>
        <p class="panel-desc">配置定时触发的自动化任务。</p>
        <div class="panel-placeholder">
          <Clock :size="48" :stroke-width="1" class="placeholder-icon"/>
          <span>定时任务功能待实现</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* ===== 左侧导航 ===== */
.settings-nav {
  width: 200px;
  min-width: 200px;
  background: #fafbfc;
  border-right: 1px solid #e6e8ec;
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  overflow-y: auto;
  flex-shrink: 0;
}

.nav-group-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 6px 16px 4px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 16px;
  border: none;
  border-radius: 0;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: #4b5563;
  text-align: left;
  transition: background 0.12s, color 0.12s;
}

.nav-btn:hover {
  background: #f0f1f3;
  color: #1f1f1f;
}

.nav-btn.active {
  background: #e8f0fe;
  color: #2563eb;
  font-weight: 500;
}

.nav-btn-icon {
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-btn.active .nav-btn-icon {
  opacity: 1;
}

.nav-divider {
  height: 1px;
  background: #e6e8ec;
  margin: 6px 12px;
}

/* ===== 右侧内容 ===== */
.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  background: #ffffff;
}

.panel {
  max-width: 640px;
}

.panel-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
}

.panel-desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 28px;
  line-height: 1.5;
}

/* ===== 默认模型配置卡片 ===== */
.config-card {
  border: 1px solid #e6e8ec;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: #fafbfc;
}

.config-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
}

.config-header .fast-icon {
  color: #10b981;
}

.config-header .translate-icon {
  color: #8b5cf6;
}

.config-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 14px;
  line-height: 1.5;
}

.config-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}

.config-label {
  width: 56px;
  font-size: 13px;
  color: #6b7280;
  flex-shrink: 0;
}

.config-value {
  font-size: 13px;
  color: #1f1f1f;
}

.config-value.placeholder {
  color: #9ca3af;
  font-style: italic;
}

/* ===== 通用占位面板 ===== */
.panel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #9ca3af;
  font-size: 13px;
}

.placeholder-icon {
  color: #d1d5db;
}
</style>
