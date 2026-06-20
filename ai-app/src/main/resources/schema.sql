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
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')), -- 创建时间
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))  -- 更新时间
);
CREATE INDEX IF NOT EXISTS idx_model_provider_code ON model_provider (code);
CREATE INDEX IF NOT EXISTS idx_model_provider_enabled ON model_provider (enabled);
