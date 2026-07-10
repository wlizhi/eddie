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

## [1.0.1-beta] - 2026-07-10

### Added

- **工具审批功能** — 新增 AbstractApprovalInterceptor 审批拦截器体系，支持 Chat/Agent 场景下工具调用的审批流程，前端新增绑定与审批交互 UI
- **工具配置命名空间** — 配置支持按工具名命名空间隔离，ConfigSchema 关联具体工具，前后端同步重构配置存储结构
- **Shell 输出字符数可配** — ShellToolConfig 新增 maxOutputChars 字段，默认 10000，支持 100~10000 范围自定义
- **WebSearch 引擎选择** — 新增 engine 参数支持指定搜索引擎（DUCKDUCKGO / BING），失败自动降级
- **开发者模式** — 新增 Agent SSE 事件 Payload 类型体系，前端设置面板增加开发者模式开关
- **多步骤轮次渲染** — 前端 AgentMessageList 重构为按 rounds 轮次渲染，thinking/toolCalls/content 独立展示
- **消息历史加载增强** — 历史消息加载时返回步骤明细与规划清单，AgentMessageVO 新增 taskPlan + stepList
- **工具调用错误反馈** — AgentChatContext 新增 toolErrorFeedback，异常工具名反馈给模型纠错
- **Markdown 图片自适应** — 图片最大宽度 100%，高度自适应，圆角 6px 边框

### Changed

- 统一工具审批拦截器为 ToolBehavior 驱动模式，消除 instanceof 判断
- 优化工具调用结果格式，增加工具名称与调用参数展示，引入多轮迭代机制
- 步骤执行从内存转为纯 DB 持久化，移除 STEP_STARTED 事件与冗余 eventType 字段
- 异常处理体系升级，新增 ModelRateLimitException 速率限制检测，前端接入限制错误展示
- WebFetch 异常处理改为 FetchResult 模式，细分 DNS、超时、SSL 等网络异常类型
- Prompt 全面优化：agent-chat/plan/execute 提示词统一，WebFetch 新增 Readability 评分
- 解决 AgentMsgStepDao N+1 查询问题，新增 findByMsgIds() 批量查询

### Fixed

- 修复工具 enabled 字段在待审批场景下的空指针问题，新增 enabledStatus 字段（0=禁用/1=启用/2=待审批）
- 修复 StepFinishTool 最后一步完成时未回填 result 到 taskPlan 的问题，前端新增结果摘要展示区域
