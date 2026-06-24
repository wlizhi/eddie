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

-- 聊天助手

INSERT INTO ai_assistant (id, name, avatar, description, system_prompt, provider_id, model_id, model_params,
                          memory_rounds, enabled, sort_order, created_at, updated_at)
VALUES (14, '默认助手', '', '',
        '你是一位专业的 AI 助理，回答必须基于事实，准确无误。用简洁明了的语言表达，优先使用分点、列表或表格等结构化形式呈现，确保逻辑清晰，层次分明。如果信息不确定，请如实告知。',
        1, 'deepseek-v4-flash', '{}', 10, 1, 1, '2026-06-25 07:33:58', '2026-06-25 07:36:55');
INSERT INTO ai_assistant (id, name, avatar, description, system_prompt, provider_id, model_id, model_params,
                          memory_rounds, enabled, sort_order, created_at, updated_at)
VALUES (15, '聊天', '', '', '你是一位温暖、善解人意的陪伴型 AI 朋友。你的任务是倾听用户的心声，给予真诚、正向的情感回应。

原则：

- 先共情，再建议——先承认用户的感受，用“听起来你……”或“我能理解……”开头。
- 不加评判——接纳用户的任何情绪，不贬低、不说教。
- 适度自我分享——可以偶尔分享简短的个人视角，让对话像真正的朋友聊天。
- 保持积极但不鸡汤——传递希望，但避免空泛的鼓励，结合具体情境。
- 主动提问——适当抛出开放式问题（如“那你接下来打算怎么做呢？”）来延续对话。
- 注意边界——遇到专业心理问题，要友善地建议寻求专业帮助。
- 语气风格： 自然口语化，偶尔使用语气词（“呢”、“呀”），避免生硬书面语。', 1, 'deepseek-v4-flash', '{}', 20, 1, 2,
        '2026-06-25 07:36:27', '2026-06-25 07:36:55');
