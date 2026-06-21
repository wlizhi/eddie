# frontend — Vue 3 前端

Vue 3 + Vite + TypeScript 前端 SPA。

构建产物输出到 `dist/`，拷贝到 `ai-app` 的 `static/` 目录。

---

## 各模块说明

### `src/api/` — API 请求层

| 文件                                     | 用途                  |
|----------------------------------------|---------------------|
| [`chat.ts`](src/api/chat.ts)           | 聊天 SSE 流式对话、消息 CRUD |
| [`assistant.ts`](src/api/assistant.ts) | 助手/智能体配置的增删改查       |
| [`session.ts`](src/api/session.ts)     | 会话列表、标题生成与重命名       |

### `src/stores/` — Pinia 状态管理

| 文件                                        | 用途             |
|-------------------------------------------|----------------|
| [`chat.ts`](src/stores/chat.ts)           | 聊天会话、消息列表、发送状态 |
| [`assistant.ts`](src/stores/assistant.ts) | 助手列表、当前选中助手    |

### `src/composables/` — 组合式逻辑

| 文件                                                           | 用途            |
|--------------------------------------------------------------|---------------|
| [`useSessionList.ts`](src/composables/useSessionList.ts)     | 会话列表加载、删除、排序  |
| [`useAssistantForm.ts`](src/composables/useAssistantForm.ts) | 助手表单校验与提交     |
| [`useRelativeTime.ts`](src/composables/useRelativeTime.ts)   | 相对时间显示（xx分钟前） |
| [`useDragSort.ts`](src/composables/useDragSort.ts)           | 拖拽排序逻辑        |

### `src/views/` — 页面视图

| 文件                                               | 用途             |
|--------------------------------------------------|----------------|
| [`ChatView.vue`](src/views/ChatView.vue)         | 主聊天页面，编排子组件    |
| [`SettingsView.vue`](src/views/SettingsView.vue) | 系统设置页面（模型、主题等） |
| [`AgentView.vue`](src/views/AgentView.vue)       | 智能体页面          |
| [`RoleListView.vue`](src/views/RoleListView.vue) | 角色列表页面         |
| [`RoleEditView.vue`](src/views/RoleEditView.vue) | 角色编辑页面         |
| [`chat/`](src/views/chat/)                       | ChatView 子组件目录 |

### `src/components/` — 通用 & 业务组件

| 文件                                                                    | 用途               |
|-----------------------------------------------------------------------|------------------|
| [`ChatSidebar.vue`](src/components/chat/ChatSidebar.vue)              | 聊天侧边栏（会话列表 + 操作） |
| [`AssistantDialog.vue`](src/components/assistant/AssistantDialog.vue) | 助手配置弹窗           |
| [`AssistantAvatar.vue`](src/components/common/AssistantAvatar.vue)    | 助手头像组件           |
| [`AvatarPicker.vue`](src/components/common/AvatarPicker.vue)          | 头像选择器（含上传）       |
| [`ImageCropper.vue`](src/components/common/ImageCropper.vue)          | 图片裁剪工具           |

### `src/types/` — TypeScript 类型

| 文件                                       | 用途       |
|------------------------------------------|----------|
| [`chat.ts`](src/types/chat.ts)           | 聊天相关类型定义 |
| [`assistant.ts`](src/types/assistant.ts) | 助手相关类型定义 |
| [`session.ts`](src/types/session.ts)     | 会话相关类型定义 |

### `src/utils/` — 工具函数

| 文件                                     | 用途                 |
|----------------------------------------|--------------------|
| [`markdown.ts`](src/utils/markdown.ts) | Markdown → HTML 渲染 |
| [`format.ts`](src/utils/format.ts)     | 时间戳格式化             |

### `src/constants/` — 常量

| 文件                                               | 用途      |
|--------------------------------------------------|---------|
| [`modelParams.ts`](src/constants/modelParams.ts) | 模型参数默认值 |
| [`theme.ts`](src/constants/theme.ts)             | 主题配置常量  |

### `src/router/` — 路由

| 文件                                | 用途              |
|-----------------------------------|-----------------|
| [`index.ts`](src/router/index.ts) | Vue Router 路由配置 |
