# Changelog

## [1.0.0] - 2026-06-30

### Added

- **多模型聊天** — 支持 DeepSeek / OpenAI 等兼容 API，对话中可随时切换模型
- **智能体骨架** — 自主规划任务、逐步执行，集成 MCP 工具调用
- **MCP 工具扩展** — 通过 MCP 协议接入 WebSearch、WebFetch 等外部工具
- **多级记忆系统** — 短期对话记忆 + 中期压缩 + 长期摘要，上下文持久化
- **纯本地运行** — 数据存储在本地 `~/.eddie/`，隐私安全
- **GraalVM Native Image 编译** — 编译为独立二进制，无需 JRE 环境
- **完整的设置面板** — 字体、主题、强调色、消息元数据显示等个性化配置
- **6 套主题配色** — 默认 / 珊瑚 / 海洋 / 薰衣草 / 玫瑰 / 星河
- **会话管理** — 多个会话创建、切换、自动标题生成、置顶
- **工具调用可视化** — SSE 流式实时展示工具执行状态

### Changed

- 重构聊天处理器链为责任链模式（PreProcessor → StreamExecutor → SseTransformer → PostProcessor）

### Fixed

- （初始版本，无历史修复记录）
