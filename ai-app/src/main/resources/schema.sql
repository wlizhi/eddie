-- 模型服务提供商表，这里控制用户启用的模型服务商及模型数据。
CREATE TABLE IF NOT EXISTS model_provider
(
    code       TEXT PRIMARY KEY,                           -- 唯一代码，如 'openai', 'deepseek'
    name       TEXT    NOT NULL,                           -- 显示名称
    base_url   TEXT    NOT NULL DEFAULT '',                -- API 基础地址
    api_key    TEXT    NOT NULL DEFAULT '',                -- API 密钥
    models     TEXT    NOT NULL DEFAULT '[]',              -- 模型 ID 列表，JSON 数组
    enabled    INTEGER NOT NULL DEFAULT 1,                 -- 0=禁用, 1=启用
    sort_order INTEGER NOT NULL DEFAULT 0,                 -- 排序序号
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')), -- 创建时间
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))  -- 更新时间
);
