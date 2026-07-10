-- ============================================================
-- Agent 智能体模块 - 独立数据库（eddie-agent.db）
-- 业务数据与主库（eddie.db）完全隔离
-- 主库建表脚本见 schema.sql
-- ============================================================

-- 智能体元数据：用户创建的 Agent 配置模板
CREATE TABLE IF NOT EXISTS ai_agent
(
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   TEXT    NOT NULL,                      -- 智能体名称
    avatar                 TEXT    NOT NULL DEFAULT '',           -- 头像
    description            TEXT    NOT NULL DEFAULT '',           -- 功能描述
    system_prompt          TEXT    NOT NULL DEFAULT '',           -- 系统提示词（任务指令）

    -- 主模型配置（规划/决策/汇总，建议推理能力强的模型）
    main_provider_id       INTEGER,                               -- 模型服务商实例 ID
    main_model_id          TEXT    NOT NULL DEFAULT '',           -- 模型 ID
    main_model_params      TEXT    NOT NULL DEFAULT '{}',         -- 模型参数 JSON

    -- 子代理模型（执行子任务，不设置默认用主模型）
    sub_provider_id        INTEGER,                               -- 模型服务商实例 ID
    sub_model_id           TEXT    NOT NULL DEFAULT '',           -- 模型 ID
    sub_model_params       TEXT    NOT NULL DEFAULT '{}',         -- 模型参数 JSON

    -- 执行控制
    semaphore              INTEGER NOT NULL DEFAULT 1,            -- 并发度
    max_iterations         INTEGER NOT NULL DEFAULT 100,          -- 最大迭代次数
    max_execution_time_sec INTEGER NOT NULL DEFAULT 300,          -- 单次执行超时（秒）
    execution_mode         TEXT    NOT NULL DEFAULT 'FOREGROUND', -- FOREGROUND / BACKGROUND

    -- 工具选择模式
    tool_selection_mode    TEXT    NOT NULL DEFAULT 'auto',

    -- 记忆轮数（每轮 = user + assistant 两条消息，0 = 无记忆）
    memory_rounds INTEGER NOT NULL DEFAULT 20,

    -- 偏好设置 JSON
    preferences            TEXT    NOT NULL DEFAULT '{}',

    -- 状态 & 排序
    enabled                INTEGER NOT NULL DEFAULT 1,
    built_in               INTEGER NOT NULL DEFAULT 0,            -- 0=用户自定义, 1=内置（不可删除）
    sort_order             INTEGER NOT NULL DEFAULT 0,

    created_at             INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at             INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

-- 智能体会话列表
CREATE TABLE IF NOT EXISTS ai_agent_session
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    agent_id      INTEGER NOT NULL,            -- 关联 ai_agent.id
    title         TEXT    NOT NULL DEFAULT '', -- 会话标题
    pinned        INTEGER NOT NULL DEFAULT 0,
    message_count INTEGER NOT NULL DEFAULT 0,  -- 外层消息数冗余
    total_tokens  INTEGER NOT NULL DEFAULT 0,
    created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_agent_session_sort ON ai_agent_session (agent_id, pinned DESC, updated_at DESC);

-- 智能体消息记录：复刻 ai_session_msg 结构，assistant_id 替换为 agent_id
-- content 存精简摘要文本，完整执行过程（多轮次/工具调用）存 ai_agent_session_msg_segment
CREATE TABLE IF NOT EXISTS ai_agent_session_msg
(
    id                         INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id                 INTEGER NOT NULL,                     -- 归属会话 ID
    agent_id                   INTEGER NOT NULL,                     -- 冗余：归属 Agent ID
    role                       TEXT    NOT NULL,                     -- user / assistant / system
    provider_id                INTEGER,                              -- 模型服务商实例 ID
    model_code                 TEXT    NOT NULL DEFAULT '',          -- 模型 code
    model_name                 TEXT    NOT NULL DEFAULT '',          -- 模型显示名称
    thinking                   TEXT    NOT NULL DEFAULT '',          -- 思考内容
    content                    TEXT    NOT NULL DEFAULT '',          -- 模型回复正文（前端对话气泡展示，完整执行过程查 segment 表）
    prompt_tokens              INTEGER NOT NULL DEFAULT 0,
    completion_tokens          INTEGER NOT NULL DEFAULT 0,
    total_tokens               INTEGER NOT NULL DEFAULT 0,
    price_estimate             REAL    NOT NULL DEFAULT 0.0,
    tool_calls                 TEXT    NOT NULL DEFAULT '[]',
    cache_read_input_tokens    INTEGER NOT NULL DEFAULT 0,
    cache_written_input_tokens INTEGER NOT NULL DEFAULT 0,
    currency                   TEXT    NOT NULL DEFAULT '',
    duration_ms                INTEGER NOT NULL DEFAULT 0,
    msg_status     TEXT    NOT NULL DEFAULT 'COMPLETED',             -- COMPLETED（完成）/ PROCESSING（AI 处理中）/ INTERRUPTED（用户中断）/ FAILED（异常终止）
    round_seq      INTEGER NOT NULL DEFAULT 0,                       -- 对话轮次标识（同一轮 user+assistant 共用 user.id，占位阶段为 0）
    iterator_state TEXT,                                             -- 迭代状态快照 JSON（闲聊时 null，任务规划时包含 agentMode/currentIterator/maxIterations）
    task_plan      TEXT,                                             -- 任务规划 JSON（闲聊时 null，任务规划时包含 title/summary/steps[...]）
    created_at     INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_agent_msg_session ON ai_agent_session_msg (session_id, id);
CREATE INDEX IF NOT EXISTS idx_agent_msg_session_round ON ai_agent_session_msg (session_id, round_seq);

-- 消息分段明细表：记录 Agent 单次执行中每一步的完整内容
CREATE TABLE IF NOT EXISTS ai_agent_session_msg_step
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    msg_id        INTEGER NOT NULL,            -- 关联 ai_agent_session_msg.id
    msg_type      INTEGER NOT NULL DEFAULT 0,  -- 消息类型，0 前端展示，1 后端任务规划
    msg_data_type INTEGER NOT NULL DEFAULT 0,  -- 消息数据类型，0 文本，1 json字符串
    step          INTEGER NOT NULL DEFAULT 0,  -- 阶段，步骤索引，对应清单中的步骤
    step_desc     TEXT    NOT NULL DEFAULT '', -- 阶段描述信息
    prompt        TEXT    NOT NULL DEFAULT '', -- 提示词
    thinking      TEXT    NOT NULL DEFAULT '', -- 思考内容
    content       TEXT    NOT NULL DEFAULT '', -- 分段完整内容
    tool_calls    TEXT    NOT NULL DEFAULT '[]',
    created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
CREATE INDEX IF NOT EXISTS idx_agent_msg_step_msg_id ON ai_agent_session_msg_step (msg_id);
