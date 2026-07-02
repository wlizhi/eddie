# Changelog

## [1.0.0-beta] - 2026-06-30

### Added

- **多模型聊天** — 支持 DeepSeek / OpenAI 等兼容 API，对话中可随时切换模型
- **完整的设置面板** — 自定义模型服务商、默认模型，字体、主题、强调色、消息元数据、显示等个性化配置
- **6 套主题配色** — 默认 / 珊瑚 / 海洋 / 薰衣草 / 玫瑰 / 星河
- **会话管理** — 多个会话创建、切换、自动标题生成、置顶
- **智能体骨架** — 自主规划任务、逐步执行，集成 MCP 工具调用
- **MCP 工具扩展** — 通过 MCP 协议接入 WebSearch、WebFetch 等外部工具
- **模型记忆** — 基础模块搭建
- **纯本地运行** — 数据存储在本地 `~/.eddie/`，隐私安全
- **GraalVM Native Image 编译** — 编译为独立二进制，无需 JRE 环境
- **Electron安装包** - 构建本地安装包
- **工具调用可视化** — SSE 流式实时展示工具执行状态

### Changed

- 重构聊天处理器链为责任链模式（PreProcessor → StreamExecutor → SseTransformer → PostProcessor）

### Fixed

- （初始版本，无历史修复记录）
