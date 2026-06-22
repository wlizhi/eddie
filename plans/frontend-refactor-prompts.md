# 前端重构 —— 逐步骤 Agent 提示文本

> 使用方式：将每个步骤的提示文本逐条粘贴到 Kilo Code（Code 模式）中执行。
> 每步完成后确认成功，再执行下一步。

---

## 步骤 1：创建 EMOJI_GROUPS 常量文件

**目标**：将 `AvatarPicker.vue` 中约 52 行的 `EMOJI_GROUPS` 数组提取到独立的常量文件。

**前置知识**：

- 项目已有 `frontend/src/constants/modelParams.ts` 和 `frontend/src/constants/theme.ts` 两个常量文件，位于
  `frontend/src/constants/` 目录
- 需要在此目录下新建 `frontend/src/constants/emojis.ts`
- 导出的变量名和类型保持与原始代码完全一致

**提示文本**：

```
在 frontend/src/constants/ 目录下新建 emojis.ts 文件。

需要从 frontend/src/components/common/AvatarPicker.vue 的 <script> 部分（第 5~56 行）提取 EMOJI_GROUPS 常量定义。

提取规则：
1. 将 const EMOJI_GROUPS: { label: string; emojis: string[] }[] = [...] 完整移到 emojis.ts
2. 在 emojis.ts 中添加 export 关键字：export const EMOJI_GROUPS: ...
3. AvatarPicker.vue 中保留 const EMOJI_GROUPS 定义，但改为 import：import { EMOJI_GROUPS } from '@/constants/emojis'
4. 不要修改 EMOJI_GROUPS 数据的任何内容
5. 不要修改 AvatarPicker.vue 的其他部分

注意：emojis.ts 中只有这一个 export，不要写多余的代码。
```

---

## 步骤 2：AvatarPicker.vue CSS 外提

**目标**：将 AvatarPicker.vue 的 `<style scoped>` 部分提取到独立的 CSS 文件。

**前置知识**：

- AvatarPicker.vue 当前 ~412 行（步骤 1 后减了 ~52 行 EMOJI 数据）
- `style scoped` 从第 223 行到第 464 行，共约 242 行 CSS
- 需要放在 `frontend/src/components/common/avatar-picker.css`
- 使用 `<style src="./avatar-picker.css" scoped/>` 语法引入
- 该文件没有 @keyframes，无需重命名

**提示文本**：

```
将 frontend/src/components/common/AvatarPicker.vue 的 <style scoped> 部分提取到独立的 CSS 文件。

操作步骤：
1. 在 frontend/src/components/common/ 目录下创建 avatar-picker.css 文件
2. 将 AvatarPicker.vue 中从 "/* scoped */" 开始到文件末尾的完整 <style scoped> 块内容（不包含 <style scoped> 和 </style> 标签本身）复制到 avatar-picker.css
3. 将 AvatarPicker.vue 中的 <style scoped>...</style> 替换为 <style src="./avatar-picker.css" scoped/>
4. avatar-picker.css 的内容无需任何改动，直接复制内部所有 CSS 规则

注意：不要修改 template 和 script 部分，只操作 style 块。
```

---

## 步骤 3：AssistantDialog.vue CSS 外提

**目标**：将 AssistantDialog.vue 的 `<style scoped>` 提取到独立 CSS 文件，并重命名 @keyframes 防冲突。

**前置知识**：

- AssistantDialog.vue 有 297 行 CSS（第 174~469 行）
- 包含 2 个 @keyframes：`error-pulse`（第 418 行）和 `nselect-error-pulse`（第 452 行）
- 需要放在 `frontend/src/components/assistant/assistant-dialog.css`
- 两个 @keyframes 必须加 `ad-` 前缀以全局防冲突

**提示文本**：

```
将 frontend/src/components/assistant/AssistantDialog.vue 的 <style scoped> 部分提取到独立 CSS 文件。

操作步骤：
1. 在 frontend/src/components/assistant/ 目录下创建 assistant-dialog.css 文件
2. 将 AssistantDialog.vue 中 <style scoped> 标签之间的所有 CSS 内容复制到 assistant-dialog.css
3. 在 copilot-dialog.css 中，将 @keyframes error-pulse 重命名为 @keyframes ad-error-pulse
4. 在 copilot-dialog.css 中，将 @keyframes nselect-error-pulse 重命名为 @keyframes ad-nselect-error-pulse
5. 在 assistant-dialog.css 中，将 animation: error-pulse 更新为 animation: ad-error-pulse
6. 在 assistant-dialog.css 中，将 animation: nselect-error-pulse 更新为 animation: ad-nselect-error-pulse
7. 将 AssistantDialog.vue 中的 <style scoped>...</style> 替换为 <style src="./assistant-dialog.css" scoped/>

注意：只修改 @keyframes 名称和 animation 引用，其他 CSS 规则不变。不要修改 template 和 script。
```

---

## 步骤 4：MessageList.vue CSS 外提

**目标**：将 MessageList.vue 的 `<style scoped>` 提取到独立 CSS 文件。

**前置知识**：

- MessageList.vue 共 385 行，CSS 部分从第 150 行到第 385 行（约 236 行）
- 包含 3 个 @keyframes：`thinking-breathe`（第 348 行）、`dot-bounce`（第 375 行）
- 需要放在 `frontend/src/views/chat/message-list.css`
- 需要加 `ml-` 前缀防全局冲突

**提示文本**：

```
将 frontend/src/views/chat/MessageList.vue 的 <style scoped> 部分提取到独立 CSS 文件。

操作步骤：
1. 在 frontend/src/views/chat/ 目录下创建 message-list.css 文件
2. 将 MessageList.vue 中 <style scoped> 标签之间的所有 CSS 内容复制到 message-list.css
3. 在 message-list.css 中，将 @keyframes thinking-breathe 重命名为 @keyframes ml-thinking-breathe
4. 在 message-list.css 中，将 animation: thinking-breathe 更新为 animation: ml-thinking-breathe
5. 在 message-list.css 中，将 @keyframes dot-bounce 重命名为 @keyframes ml-dot-bounce
6. 在 message-list.css 中，将 animation: dot-bounce 更新为 animation: ml-dot-bounce
7. 将 MessageList.vue 中的 <style scoped>...</style> 替换为 <style src="./message-list.css" scoped/>

注意：只修改 @keyframes 名称和 animation 引用，其他 CSS 规则不变。不要修改 template 和 script。
```

---

## 步骤 5：InputArea.vue CSS 外提

**目标**：将 InputArea.vue 的 `<style scoped>` 提取到独立 CSS 文件。

**前置知识**：

- InputArea.vue 共 378 行，CSS 部分从第 208 行到第 378 行（约 172 行）
- 没有 @keyframes，无需重命名
- 需要放在 `frontend/src/views/chat/input-area.css`

**提示文本**：

```
将 frontend/src/views/chat/InputArea.vue 的 <style scoped> 部分提取到独立 CSS 文件。

操作步骤：
1. 在 frontend/src/views/chat/ 目录下创建 input-area.css 文件
2. 将 InputArea.vue 中 <style scoped> 标签之间的所有 CSS 内容复制到 input-area.css
3. 将 InputArea.vue 中的 <style scoped>...</style> 替换为 <style src="./input-area.css" scoped/>

注意：该 CSS 文件没有 @keyframes，直接复制无需修改。不要修改 template 和 script 部分。
```

---

## 步骤 6：创建 SettingsView 所有面板组件 + CSS

**目标**：将 SettingsView.vue 的 8 个 `v-if` 面板拆分为独立组件，CSS 外提，骨架改用动态组件。

**前置知识**：

- SettingsView.vue 当前 375 行，包含导航 + 8 个 `v-if` 面板 + 1 个完整 CSS
- 8 个面板中 7 个是占位符（待实现），只有 `default-model` 面板有实际内容
- 所有面板之间无共享状态，仅通过 `activeKey` 切换
- 拆分为 2 个子步骤：① 创建面板文件 ② 改造骨架

### 步骤 6a：创建所有面板组件文件

**提示文本**：

```
在 frontend/src/views/settings/ 目录下创建以下 8 个面板组件文件。
这些文件的内容直接从 frontend/src/views/SettingsView.vue 的对应 v-if 块复制。

创建前先阅读 SettingsView.vue 获取各面板内容。

===== 文件 1：frontend/src/views/settings/ModelProviderPanel.vue =====

<template>
  <div class="panel">
    <h3 class="panel-title">模型服务</h3>
    <p class="panel-desc">管理模型服务商，启用/禁用、添加自定义兼容服务。</p>
    <div class="panel-placeholder">
      <Cpu :size="48" :stroke-width="1" class="placeholder-icon"/>
      <span>模型服务商管理功能待实现</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {Cpu} from '@lucide/vue'
</script>

<style scoped>
/* 所有样式将在后续统一提取到 settings.css，暂时使用内联 */
.panel { max-width: 640px; }
.panel-title { font-size: 22px; font-weight: 600; color: #1f1f1f; margin-bottom: 6px; }
.panel-desc { font-size: 13px; color: #6b7280; margin-bottom: 28px; line-height: 1.5; }
.panel-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 60px 20px; color: #9ca3af; font-size: 13px; }
.placeholder-icon { color: #d1d5db; }
</style>


===== 文件 2：frontend/src/views/settings/DefaultModelPanel.vue =====

将 SettingsView.vue 第 79~130 行的 default-model 面板的 template 完整复制过来，加上对应的 script setup 导入 Zap、Globe 图标。

<template>
  <div class="panel">
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
</template>

<script setup lang="ts">
import {Zap, Globe} from '@lucide/vue'
</script>

<style scoped>
.panel { max-width: 640px; }
.panel-title { font-size: 22px; font-weight: 600; color: #1f1f1f; margin-bottom: 6px; }
.panel-desc { font-size: 13px; color: #6b7280; margin-bottom: 28px; line-height: 1.5; }
.config-card { border: 1px solid #e6e8ec; border-radius: 10px; padding: 20px 24px; margin-bottom: 16px; background: #fafbfc; }
.config-header { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1f1f1f; margin-bottom: 6px; }
.config-header .fast-icon { color: #10b981; }
.config-header .translate-icon { color: #8b5cf6; }
.config-hint { font-size: 12px; color: #9ca3af; margin-bottom: 14px; line-height: 1.5; }
.config-row { display: flex; align-items: center; gap: 12px; padding: 6px 0; }
.config-label { width: 56px; font-size: 13px; color: #6b7280; flex-shrink: 0; }
.config-value { font-size: 13px; color: #1f1f1f; }
.config-value.placeholder { color: #9ca3af; font-style: italic; }
</style>


===== 文件 3~8：占位面板组件 =====

以下 6 个面板结构完全相同，只是 title、desc、图标不同。每个文件不到 15 行。

文件列表：
- frontend/src/views/settings/GeneralPanel.vue         ← 图标: Settings, 标题: 常规设置, 描述: 应用的通用行为和偏好配置。
- frontend/src/views/settings/DisplayPanel.vue         ← 图标: Palette, 标题: 显示设置, 描述: 主题、字体、布局等显示偏好。
- frontend/src/views/settings/McpPanel.vue             ← 图标: Network, 标题: MCP 服务器, 描述: 管理 MCP (Model Context Protocol) 服务器连接。
- frontend/src/views/settings/SkillsPanel.vue          ← 图标: Puzzle, 标题: 技能, 描述: 管理和配置助手可用的技能模块。
- frontend/src/views/settings/WebSearchPanel.vue       ← 图标: Search, 标题: 网络搜索, 描述: 配置搜索引擎和网络搜索参数。
- frontend/src/views/settings/ChannelsPanel.vue        ← 图标: Radio, 标题: 频道, 描述: 接入外部消息频道（如 QQ 机器人、飞书、Discord 等）。
- frontend/src/views/settings/ScheduledTasksPanel.vue  ← 图标: Clock, 标题: 定时任务, 描述: 配置定时触发的自动化任务。

每个占位面板的模板格式如下（以 ModelProviderPanel 为模板，替换图标、标题、描述）：

<template>
  <div class="panel">
    <h3 class="panel-title">{{标题}}</h3>
    <p class="panel-desc">{{描述}}</p>
    <div class="panel-placeholder">
      <{{图标组件}} :size="48" :stroke-width="1" class="placeholder-icon"/>
      <span>{{标题}}功能待实现</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { {{图标组件}} } from '@lucide/vue'
</script>

<style scoped>
.panel { max-width: 640px; }
.panel-title { font-size: 22px; font-weight: 600; color: #1f1f1f; margin-bottom: 6px; }
.panel-desc { font-size: 13px; color: #6b7280; margin-bottom: 28px; line-height: 1.5; }
.panel-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 60px 20px; color: #9ca3af; font-size: 13px; }
.placeholder-icon { color: #d1d5db; }
</style>

注意：每个占位面板的 script setup 只导入自己需要的图标组件。
```

### 步骤 6b：提取 settings.css 并改造 SettingsView.vue 骨架

**提示文本**：

```
先创建 frontend/src/views/settings/settings.css 文件。

将 SettingsView.vue 当前 <style scoped> 中的所有 CSS 复制到 settings.css 中（约 170 行，第 205~375 行）。

这个 CSS 中没有 @keyframes，无需重命名。

然后，将 SettingsView.vue 完全重写为骨架模式，使用动态组件 <component :is=""> 替代 8 个 v-if。

新的 SettingsView.vue 骨架内容如下：

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Clock, Cpu, Globe, Monitor, Network, Palette, Puzzle, Radio, Search, Settings, Zap } from '@lucide/vue'
import ModelProviderPanel from './settings/ModelProviderPanel.vue'
import DefaultModelPanel from './settings/DefaultModelPanel.vue'
import GeneralPanel from './settings/GeneralPanel.vue'
import DisplayPanel from './settings/DisplayPanel.vue'
import McpPanel from './settings/McpPanel.vue'
import SkillsPanel from './settings/SkillsPanel.vue'
import WebSearchPanel from './settings/WebSearchPanel.vue'
import ChannelsPanel from './settings/ChannelsPanel.vue'
import ScheduledTasksPanel from './settings/ScheduledTasksPanel.vue'

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
      { key: 'model-provider', label: '模型服务', icon: Cpu },
      { key: 'default-model', label: '默认模型', icon: Zap },
    ],
  },
  {
    label: '通用设置',
    items: [
      { key: 'general', label: '常规设置', icon: Settings },
      { key: 'display', label: '显示设置', icon: Monitor },
    ],
  },
  {
    label: '扩展功能',
    items: [
      { key: 'mcp', label: 'MCP 服务器', icon: Network },
      { key: 'skills', label: '技能', icon: Puzzle },
      { key: 'web-search', label: '网络搜索', icon: Globe },
      { key: 'channels', label: '频道', icon: Radio },
      { key: 'scheduled-tasks', label: '定时任务', icon: Clock },
    ],
  },
]

const panelMap: Record<string, any> = {
  'model-provider': ModelProviderPanel,
  'default-model': DefaultModelPanel,
  'general': GeneralPanel,
  'display': DisplayPanel,
  'mcp': McpPanel,
  'skills': SkillsPanel,
  'web-search': WebSearchPanel,
  'channels': ChannelsPanel,
  'scheduled-tasks': ScheduledTasksPanel,
}

const activeKey = ref<string>('model-provider')
const currentPanel = computed(() => panelMap[activeKey.value])
</script>

<template>
  <div class="settings-layout">
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
    <div class="settings-content">
      <component :is="currentPanel"/>
    </div>
  </div>
</template>

<style src="./settings/settings.css" scoped/>

注意：
- 完整保留左侧导航的 template 不变
- 原来的 8 个 <div v-if="..."> 面板全部移除，替换为一行 <component :is="currentPanel"/>
- 引入外部 CSS：<style src="./settings/settings.css" scoped/>
- script 中去掉原来的单个 ref + 8 个 v-if，改为 panelMap 映射 + computed
```
