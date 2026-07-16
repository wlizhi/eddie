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

## [1.0.3-beta] - 2026-07-16

### Added

- **划词助手重构** — 基于 Vue 弹窗替代旧版 iframe，新增紧凑工具栏模式（compact）、自定义滚动条样式、设置面板
- **内置本地数据库查询工具** — LocalDbQueryTools 支持通过自然语言查询本地 SQLite 数据，使用 DataSource 连接池运行
- **Web Fetch 智能摘要** — 获取内容超出长度限制时自动调用 LLM 摘要，新增 `purpose` 参数引导摘要方向
- **手动模式工具预填充** — 切入手动模式时自动预选已启用的 MCP 工具，无需重复勾选
- **标题生成配置** — 支持自定义标题生成轮数与角色标签（智能体 / 助手），标题长度上限调整为 20 字
- **消息事件模型信息** — `message_created` SSE 事件中透传 `modelCode` / `modelName`，前端实时显示模型标识
- **Agent 迭代状态持久化** — 引入 `iterator_state` 持久化 Agent 迭代终止状态，支持中断恢复机制
- **对话轮次标识 round_seq** — 引入 `round_seq` 轮次标识，优化消息回填与短程记忆加载，新增游标分页查询
- **macOS 原生全屏适配** — 全屏时自动隐藏 TitleBar，内容铺满屏幕
- **Electron 导航安全** — 拦截 `will-navigate` 事件防止窗口导航至外部 URL
- **规划模式复制增强** — Agent 复制功能支持 `taskPlan.result` 优先复制
- **代码块一键复制** — Markdown 代码块新增复制按钮，支持 clipboard API 降级
- **MCP 工具结果展示优化** — 解析 MCP 工具 JSON 包裹格式，提取纯文本内容展示
- **模型信息展示** — 更新 Kimi API 地址至最新 OpenAI 兼容端点

### Changed

- **Electron 模块化重构** — 扁平结构重组成 `src/` 下模块化目录（main / preload / services / selection-assistant）
- **AgentChatContext 拆分子上下文** — 拆分为 `AgentEventContext` / `AgentOutputContext` / `AgentMetrics`，提取工具截断配置预处理器
- **步骤字段重命名** — `step` → `stepNumber`，`stepId` → `stepRecordId`，数据库与前端同步更新
- **工具返回值统一** — 工具方法返回值从 `ApiResult<String>` 统一改为 `String`
- **SSE Payload 类名规范化** — 统一为 `AgentXxxPayload` / `ChatXxxPayload` 前缀命名
- **前端共享组件抽取** — 提取 `AgentThinkingBlock` / `AgentToolCard` / `AgentContentBlock` 共享组件，消除 Agent 与 Chat 消息列表重复渲染逻辑
- **工具结果展示重构** — 新增 "→ 参数" / "← 结果" 标签区分，提取 `formatToolResult()` 独立模块
- **用户消息纯文本化** — 用户消息改用纯文本 `div` 展示，移除 `AgentContentBlock` 渲染
- **AOT 反射注册重构** — 优化 GraalVM Native Image 兼容性
- **设置导航简化** — 移除设置面板分组标签，简化导航结构
- **移除 streamUsage 配置项** — 清理不再使用的流式用量配置
- **自定义滚动条归并** — 提取到全局 `theme.css` 统一管理
- **消息渲染缓存** — `renderMd` 添加 LRU 缓存（上限 300），避免同一 Markdown 反复解析

### Fixed

- **LocalDbQueryTools 连接池修复** — 改用 HikariCP DataSource 替代 `DriverManager.getConnection()`，修复 GraalVM Native Image 下 JDBC 驱动加载失败
- **JSON 双重序列化修复** — 新增 `JsonUtil.unwrapJsonString()` 解包工具返回的二次序列化字符串
- **formatToolResult 解析修复** — 修复 `fixNewlines` 过早调用导致 JSON 解析失败的问题
- **窄屏消息溢出修复** — 修复窄屏模式下消息行溢出及 px 转 rem 适配问题
- **MCP 资源路径修复** — 修正 AOT 编译资源包含路径 `db/init/*.sql`
- **ShellTools Windows 编码适配** — 修复 Windows 环境下 UTF-8 编码兼容性，使用 `Path.of` 拼接路径
- **Tomcat UTF-8 编码配置** — 添加 servlet 编码与 content-type 配置

### Perf

- **SQLite 页面缓存优化** — 主数据源与 Agent 数据源 `page_size` 提升至 16384，`cache_size` 提升至 30000
