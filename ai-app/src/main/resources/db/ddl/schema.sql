-- 模型服务提供商表，这里控制用户启用的模型服务商及模型数据。
-- 内置主流厂商（built_in=1，不可删除，可启用/禁用），用户可自定义添加兼容服务（built_in=0，可删除/编辑）。
CREATE TABLE IF NOT EXISTS model_provider
(
    id       INTEGER PRIMARY KEY AUTOINCREMENT,                         -- 自增主键
    code     TEXT    NOT NULL,                                          -- 业务类型代码，如 'openai', 'deepseek'，用于匹配 ChatPolicy
    name     TEXT    NOT NULL,                                          -- 显示名称，如 '我的本地Ollama'
    base_url   TEXT    NOT NULL DEFAULT '',                -- API 基础地址
    api_key    TEXT    NOT NULL DEFAULT '',                -- API 密钥
    models   TEXT    NOT NULL DEFAULT '[]',                             -- 该实例下的模型 ID 列表，JSON 数组
    enabled    INTEGER NOT NULL DEFAULT 1,                 -- 0=禁用, 1=启用
    built_in INTEGER NOT NULL DEFAULT 0,                                -- 0=用户自定义(可删除/编辑), 1=内置(不可删除,可启/禁)
    sort_order INTEGER NOT NULL DEFAULT 0,                 -- 排序序号
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000), -- 创建时间（毫秒时间戳）
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)  -- 更新时间（毫秒时间戳）
);
CREATE INDEX IF NOT EXISTS idx_model_provider_code ON model_provider (code);
CREATE INDEX IF NOT EXISTS idx_model_provider_enabled ON model_provider (enabled);

-- 助手列表
CREATE TABLE IF NOT EXISTS ai_assistant
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    avatar        TEXT    NOT NULL DEFAULT '',
    description   TEXT    NOT NULL DEFAULT '',
    system_prompt TEXT    NOT NULL DEFAULT '',

    -- 模型配置（provider_id 关联 model_provider.id，精确到服务商实例）
    provider_id   INTEGER,
    model_id      TEXT    NOT NULL DEFAULT '',

    -- 模型参数 JSON：{"temperature":0.7, "maxTokens":2048, "topP":0.9, ...}
    model_params  TEXT    NOT NULL DEFAULT '{}',

    -- 助手偏好设置 JSON：{"webSearchEnabled":true, "mcpToolMode":"auto", ...}
    preferences TEXT NOT NULL DEFAULT '{}',

    -- 记忆轮数
    memory_rounds INTEGER NOT NULL DEFAULT 20,

    -- 状态 & 排序
    enabled       INTEGER NOT NULL DEFAULT 1,
    sort_order    INTEGER NOT NULL DEFAULT 0,

    -- 工具选择模式：auto（自动选择）、manual（手动选择）、none（不使用工具）
    tool_selection_mode TEXT NOT NULL DEFAULT 'none',

    created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_assistant_enabled ON ai_assistant (enabled);

-- 会话列表：每个助手可创建多个会话，按置顶 → 更新时间倒序
CREATE TABLE IF NOT EXISTS ai_session
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    assistant_id  INTEGER NOT NULL,            -- 归属助手 ID
    title         TEXT    NOT NULL DEFAULT '', -- AI 生成的会话标题（默认为空，首轮对话后生成）
    pinned        INTEGER NOT NULL DEFAULT 0,  -- 0=普通, 1=置顶
    message_count INTEGER NOT NULL DEFAULT 0,  -- 消息数量冗余字段，每次发消息时同步更新
    total_tokens INTEGER NOT NULL DEFAULT 0,   -- 累计 token 数冗余字段
    created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_session_sort ON ai_session (assistant_id, pinned DESC, updated_at DESC);

-- 消息记录：每个会话的全量对话记录 + Token/费用统计
CREATE TABLE IF NOT EXISTS ai_session_msg
(
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id        INTEGER NOT NULL,             -- 归属会话 ID
    assistant_id      INTEGER NOT NULL,             -- 冗余：归属助手 ID
    role              TEXT    NOT NULL,             -- user / assistant / system
    provider_id       INTEGER,                      -- 模型服务商实例 ID
    model_code        TEXT    NOT NULL DEFAULT '',  -- 模型 code，如 "deepseek-v4-pro"
    model_name        TEXT    NOT NULL DEFAULT '',  -- 模型显示名称
    thinking          TEXT    NOT NULL DEFAULT '',  -- 思考内容（DeepSeek reasoning_content）
    content           TEXT    NOT NULL,             -- 消息正文
    prompt_tokens     INTEGER NOT NULL DEFAULT 0,   -- 提示 token 数
    completion_tokens INTEGER NOT NULL DEFAULT 0,   -- 完成 token 数
    total_tokens      INTEGER NOT NULL DEFAULT 0,   -- 总 token 数
    price_estimate    REAL    NOT NULL DEFAULT 0.0, -- 预估费用（美元）
    tool_calls TEXT NOT NULL DEFAULT '[]',          -- 工具调用记录 JSON 数组
    cache_read_input_tokens INTEGER NOT NULL DEFAULT 0, -- 缓存读取的 input token 数（来自 Usage）
    cache_written_input_tokens INTEGER NOT NULL DEFAULT 0, -- 缓存写入的 input token 数（来自 Usage）
    currency                TEXT    NOT NULL DEFAULT '', -- 费用货币符号，如 ¥ / $
    duration_ms INTEGER NOT NULL DEFAULT 0, -- 接口耗时（毫秒）
    msg_status              TEXT    NOT NULL DEFAULT 'COMPLETED', -- 消息状态：COMPLETED（正常完成）/ STREAMING（流式进行中）/ INTERRUPTED（中断）
    created_at        INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_msg_session_id ON ai_session_msg (session_id, id);
CREATE INDEX IF NOT EXISTS idx_msg_status ON ai_session_msg (msg_status);

-- 全局配置表（key-value 结构，config_key 直接使用 GlobalConfigKey 枚举名存储）
-- config_val 为 JSON 字符串，前端/业务模块按需反序列化
-- config_type 标明配置归属：FRONTEND（前端可见）/ BACKEND（后端内置，不返回前端）
CREATE TABLE IF NOT EXISTS global_config
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key  TEXT NOT NULL UNIQUE,
    config_val  TEXT NOT NULL DEFAULT '{}',
    config_type TEXT NOT NULL DEFAULT 'FRONTEND',
    description TEXT NOT NULL DEFAULT '',
    updated_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

-- 初始数据（仅首次建表时插入）
INSERT INTO global_config (config_key, config_val, config_type, description)
VALUES ('DEFAULT_MODEL', '{}', 'FRONTEND', '默认对话模型'),
       ('FAST_MODEL', '{}', 'FRONTEND', '快速模型'),
       ('TRANSLATE_MODEL', '{}', 'FRONTEND', '翻译模型'),
       ('GENERAL_SETTINGS',
        '{"searchResultCount":8,"webFetchMaxChars":8000,"enableAutoTitle":true,"titleGenerationRounds":1}',
        'FRONTEND', '常规设置'),
       ('TOOL_CALL_MAX_LENGTH', '20000', 'BACKEND', '工具调用响应最大长度')
ON CONFLICT(config_key) DO NOTHING;

-- 工具定义表：注册系统中可用的工具（内置工具或 MCP 工具）
CREATE TABLE IF NOT EXISTS ai_tool_definition
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    tool_type     TEXT    NOT NULL DEFAULT 'BUILT_IN', -- BUILT_IN（内置）/ MCP（MCP 工具）
    name TEXT NOT NULL,                                -- 工具唯一标识名，如 'web_search', 'file_read'（同一 MCP 服务内唯一）
    display_name  TEXT    NOT NULL DEFAULT '',         -- 工具显示名称
    description   TEXT    NOT NULL DEFAULT '',         -- 工具功能描述
    enabled       INTEGER NOT NULL DEFAULT 1,          -- 0=禁用, 1=启用
    built_in      INTEGER NOT NULL DEFAULT 0,          -- 0=用户自定义, 1=内置（不可删除）
    mcp_server_id INTEGER,                             -- 关联 ai_mcp_server.id（仅 MCP 类型）
    sort_order    INTEGER NOT NULL DEFAULT 0,          -- 排序序号
    created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_tool_def_type ON ai_tool_definition (tool_type);
CREATE INDEX IF NOT EXISTS idx_tool_def_enabled ON ai_tool_definition (enabled);
CREATE INDEX IF NOT EXISTS idx_tool_def_mcp_server ON ai_tool_definition (mcp_server_id);
-- 内置工具（mcp_server_id IS NULL）名称全局唯一；MCP 工具（mcp_server_id IS NOT NULL）在同一 MCP 服务内唯一
CREATE UNIQUE INDEX IF NOT EXISTS idx_tool_def_name_builtin ON ai_tool_definition (name) WHERE mcp_server_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_tool_def_name_mcp ON ai_tool_definition (name, mcp_server_id) WHERE mcp_server_id IS NOT NULL;

-- 工具绑定表：Owner（助手/智能体）与工具的关联关系（多态关联）
CREATE TABLE IF NOT EXISTS ai_owner_tool_binding
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_type TEXT    NOT NULL DEFAULT 'ASSISTANT', -- ASSISTANT（助手）/ AGENT（智能体）
    owner_id   INTEGER NOT NULL,                     -- 归属方 ID（assistant_id / agent_id）
    tool_id    INTEGER NOT NULL,                     -- 工具 ID
    enabled    INTEGER NOT NULL DEFAULT 1,           -- 0=禁用, 1=启用
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    UNIQUE (owner_type, owner_id, tool_id)           -- 同一归属方不可重复绑定同一工具
);
CREATE INDEX IF NOT EXISTS idx_owner_tool_binding_owner ON ai_owner_tool_binding (owner_type, owner_id);
CREATE INDEX IF NOT EXISTS idx_owner_tool_binding_tool ON ai_owner_tool_binding (tool_id);

-- MCP 服务器配置表：管理 MCP 服务端连接配置
CREATE TABLE IF NOT EXISTS ai_mcp_server
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,                 -- MCP 服务端名称
    description            TEXT NOT NULL DEFAULT '',  -- MCP 服务描述
    source_type   TEXT NOT NULL DEFAULT 'USER',       -- 来源类型：BUILT_IN（内置工具）/ USER（用户自定义）/ PROVIDER（第三方服务商）
    source_config TEXT NOT NULL DEFAULT '{}',         -- 来源配置 JSON（多态），PROVIDER 类型存储认证信息及服务商元信息
    transport_type  TEXT    NOT NULL DEFAULT 'STDIO', -- 传输方式：STDIO / SSE / STREAMABLE_HTTP

    -- STDIO 专用参数
    command         TEXT    NOT NULL DEFAULT '',      -- STDIO 启动命令，如 'npx'
    args            TEXT    NOT NULL DEFAULT '[]',    -- STDIO 命令参数，JSON 数组
    env             TEXT    NOT NULL DEFAULT '{}',    -- STDIO 环境变量，JSON 对象

    -- SSE / Streamable HTTP 专用参数
    url             TEXT    NOT NULL DEFAULT '',      -- SSE/HTTP 服务端 URL
    headers                TEXT NOT NULL DEFAULT '{}', -- SSE/HTTP 自定义请求头，JSON 对象

    timeout_seconds INTEGER NOT NULL DEFAULT 60,      -- 请求超时时间（秒）
    enabled         INTEGER NOT NULL DEFAULT 1,       -- 0=禁用, 1=启用
    sort_order      INTEGER NOT NULL DEFAULT 0,       -- 排序序号

    -- 断开重连配置
    reconnect_interval_sec INTEGER, -- 重连间隔(秒)，NULL/0=默认5秒
    max_reconnect_attempts INTEGER, -- 最大重试次数，NULL/0=无限重试

    created_at      INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at      INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_mcp_server_source_type ON ai_mcp_server (source_type);
CREATE INDEX IF NOT EXISTS idx_mcp_server_enabled ON ai_mcp_server (enabled);

