# Chat 视图组件

## 组件树

```
ChatView.vue（编排器，60 行）
├── Toolbar.vue      ← 右上角悬浮工具栏（50 行）
├── MessageList.vue  ← 消息列表（150 行）
├── EmptyState.vue   ← 空状态引导（60 行）
└── InputArea.vue    ← 底部输入区 + 模型选择器（140 行）
```

## 数据流

- **Pinia Store** (`useChatStore()`)：所有子组件直接从 store 读取数据
- **Props + Emits**：仅用于组件局部状态（宽屏模式、问答模式、输入框文本）

## 组件职责

| 组件          | 职责                                      | 关键 Props   | 关键 Emits                 |
|-------------|-----------------------------------------|------------|--------------------------|
| ChatView    | 状态编排，持有 isWideMode/isChatMode/inputText | —          | —                        |
| Toolbar     | 宽屏/问答模式切换按钮                             | wide, chat | update:wide, update:chat |
| MessageList | 消息渲染、thinking 折叠、自动滚动、元数据展示             | qaMode     | —                        |
| EmptyState  | 欢迎界面 + 快捷建议问题                           | —          | selectSuggestion         |
| InputArea   | 文本输入框、发送/中断、模型选择器、功能开关                  | modelValue | update:modelValue, send  |

## 工具函数

| 文件                                             | 导出             | 说明                 |
|------------------------------------------------|----------------|--------------------|
| [`utils/markdown.ts`](../../utils/markdown.ts) | `renderMd()`   | Markdown → HTML 渲染 |
| [`utils/format.ts`](../../utils/format.ts)     | `formatTime()` | 时间戳 → 格式化日期字符串     |
