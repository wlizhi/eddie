-- 内置模型服务商初始化数据（v1）
-- 首次执行时插入三条内置记录，后续启动时 version >= 1 已执行则跳过
-- 时间字段由数据库自动填充，与表 DEFAULT 保持一致

INSERT INTO model_provider (code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES ('deepseek', 'DeepSeek', 'https://api.deepseek.com', '',
        '[{"id":"deepseek-v4-flash","name":"deepseek-v4-flash","object":"model","owned_by":"deepseek","capabilities":["function_calling"],"currency":"¥","input_price":1.0,"output_price":2.0},{"id":"deepseek-v4-pro","name":"deepseek-v4-pro","object":"model","owned_by":"deepseek","capabilities":["function_calling"]}]',
        1, 1, 1, datetime('now', 'localtime'), datetime('now', 'localtime'));

INSERT INTO model_provider (code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES ('dashscope', '阿里云百炼', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '',
        '[{"id":"qwen3.5-flash","name":"qwen3.5-flash","object":"model","owned_by":"system","capabilities":["function_calling"],"currency":"¥","input_price":0.2,"output_price":2.0},{"id":"qwen3.7-max","name":"qwen3.7-max","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.7-plus","name":"qwen3.7-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.6-plus","name":"qwen3.6-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.6-flash","name":"qwen3.6-flash","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]}]',
        1, 1, 3, datetime('now', 'localtime'), datetime('now', 'localtime'));

INSERT INTO model_provider (code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES ('siliconflow', '硅基流动', 'https://api.siliconflow.cn/v1', '',
        '[{"id":"zai-org/GLM-5.2","name":"zai-org/GLM-5.2","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"deepseek-ai/DeepSeek-V4-Pro","name":"deepseek-ai/DeepSeek-V4-Pro","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"deepseek-ai/DeepSeek-V4-Flash","name":"deepseek-ai/DeepSeek-V4-Flash","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"Pro/moonshotai/Kimi-K2.6","name":"Pro/moonshotai/Kimi-K2.6","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"Pro/zai-org/GLM-5.1","name":"Pro/zai-org/GLM-5.1","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"nex-agi/Nex-N2-Pro","name":"nex-agi/Nex-N2-Pro","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]}]',
        1, 1, 2, datetime('now', 'localtime'), datetime('now', 'localtime'));
