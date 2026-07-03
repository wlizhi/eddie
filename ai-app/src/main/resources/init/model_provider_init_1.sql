-- 内置模型服务商初始化数据（v1）
-- 首次执行时插入三条内置记录，后续启动时 version >= 1 已执行则跳过
-- 时间字段由数据库自动填充，与表 DEFAULT 保持一致

INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (1, 'deepseek', 'DeepSeek', 'https://api.deepseek.com', '',
        '[{"id":"deepseek-v4-flash","name":"deepseek-v4-flash","object":"model","owned_by":"deepseek","capabilities":["function_calling"],"currency":"¥","input_price":1.0,"output_price":2.0,"cache_input_price":0.02,"cache_write_input_price":null},{"id":"deepseek-v4-pro","name":"deepseek-v4-pro","object":"model","owned_by":"deepseek","capabilities":["function_calling"],"currency":"¥","input_price":3.0,"output_price":6.0,"cache_input_price":0.025,"cache_write_input_price":null}]',
        1, 1, 1);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (2, 'dashscope', '阿里云百炼', 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        '',
        '[{"id":"qwen3.5-flash","name":"qwen3.5-flash","object":"model","owned_by":"system","capabilities":["function_calling"],"currency":"¥","input_price":0.2,"output_price":2.0},{"id":"qwen3.6-plus","name":"qwen3.6-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.7-plus","name":"qwen3.7-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.7-max","name":"qwen3.7-max","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]}]',
        1, 1, 3);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (3, 'siliconflow', '硅基流动', 'https://api.siliconflow.cn/v1',
        '',
        '[{"id":"Qwen/Qwen3-8B","name":"Qwen/Qwen3-8B","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"THUDM/GLM-4-9B-0414","name":"THUDM/GLM-4-9B-0414","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"THUDM/GLM-Z1-9B-0414","name":"THUDM/GLM-Z1-9B-0414","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]}]',
        1, 1, 2);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (4, 'openai', 'OpenAI', 'https://api.openai.com/v1', '',
        '[{"id":"gpt-5.5","name":"gpt-5.5","object":"model","owned_by":"openai","capabilities":["function_calling"]},{"id":"gpt-5.4","name":"gpt-5.4","object":"model","owned_by":"openai","capabilities":["function_calling"]},{"id":"gpt-5.4-mini","name":"gpt-5.4-mini","object":"model","owned_by":"openai","capabilities":["function_calling"]},{"id":"gpt-5.4-nano","name":"gpt-5.4-nano","object":"model","owned_by":"openai","capabilities":["function_calling"]},{"id":"o3","name":"o3","object":"model","owned_by":"openai","capabilities":["function_calling","reasoning"]},{"id":"o4-mini","name":"o4-mini","object":"model","owned_by":"openai","capabilities":["function_calling","reasoning"]},{"id":"gpt-4.1","name":"gpt-4.1","object":"model","owned_by":"openai","capabilities":["function_calling"]},{"id":"gpt-4o","name":"gpt-4o","object":"model","owned_by":"openai","capabilities":["function_calling","vision"]},{"id":"gpt-4o-mini","name":"gpt-4o-mini","object":"model","owned_by":"openai","capabilities":["function_calling","vision"]}]',
        1, 1, 4);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (5, 'anthropic', 'Anthropic', 'https://api.anthropic.com', '',
        '[{"id":"claude-fable-5","name":"claude-fable-5","object":"model","owned_by":"anthropic","capabilities":["function_calling","vision"]},{"id":"claude-opus-4-8","name":"claude-opus-4-8","object":"model","owned_by":"anthropic","capabilities":["function_calling","vision"]},{"id":"claude-sonnet-5","name":"claude-sonnet-5","object":"model","owned_by":"anthropic","capabilities":["function_calling","vision"]},{"id":"claude-haiku-4-5","name":"claude-haiku-4-5","object":"model","owned_by":"anthropic","capabilities":["function_calling","vision"]}]',
        1, 1, 5);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (6, 'google', 'Google', 'https://generativelanguage.googleapis.com/v1beta/openai/', '',
        '[{"id":"gemini-2.5-pro","name":"gemini-2.5-pro","object":"model","owned_by":"google","capabilities":["function_calling","vision","web_search"]},{"id":"gemini-2.5-flash","name":"gemini-2.5-flash","object":"model","owned_by":"google","capabilities":["function_calling","vision"]},{"id":"gemini-2.0-flash","name":"gemini-2.0-flash","object":"model","owned_by":"google","capabilities":["function_calling","vision"]},{"id":"gemini-2.0-flash-lite","name":"gemini-2.0-flash-lite","object":"model","owned_by":"google","capabilities":["function_calling","vision"]}]',
        1, 1, 6);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (7, 'xai', 'xAI Grok', 'https://api.x.ai', '',
        '[{"id":"grok-3","name":"grok-3","object":"model","owned_by":"xai","capabilities":["function_calling"]},{"id":"grok-3-mini","name":"grok-3-mini","object":"model","owned_by":"xai","capabilities":["function_calling"]},{"id":"grok-2","name":"grok-2","object":"model","owned_by":"xai","capabilities":["function_calling"]},{"id":"grok-2-vision","name":"grok-2-vision","object":"model","owned_by":"xai","capabilities":["function_calling","vision"]}]',
        1, 1, 7);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (8, 'mistral', 'Mistral', 'https://api.mistral.ai/v1', '',
        '[{"id":"mistral-large","name":"mistral-large","object":"model","owned_by":"mistral","capabilities":["function_calling"]},{"id":"mistral-small","name":"mistral-small","object":"model","owned_by":"mistral","capabilities":["function_calling"]},{"id":"codestral","name":"codestral","object":"model","owned_by":"mistral","capabilities":["function_calling"]},{"id":"ministral-8b","name":"ministral-8b","object":"model","owned_by":"mistral","capabilities":["function_calling"]}]',
        1, 1, 8);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (9, 'zhipu', '智谱AI', 'https://open.bigmodel.cn/api/paas/v4', '',
        '[{"id":"GLM-5.1","name":"GLM-5.1","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-5","name":"GLM-5","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-5-Turbo","name":"GLM-5-Turbo","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-5V-Turbo","name":"GLM-5V-Turbo","object":"model","owned_by":"zhipu","capabilities":["function_calling","vision"]},{"id":"GLM-4.7","name":"GLM-4.7","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-4.7-Flash","name":"GLM-4.7-Flash","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-4.5-Flash","name":"GLM-4.5-Flash","object":"model","owned_by":"zhipu","capabilities":["function_calling"]},{"id":"GLM-4.5-Air","name":"GLM-4.5-Air","object":"model","owned_by":"zhipu","capabilities":["function_calling"]}]',
        1, 1, 9);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (10, 'moonshot', 'Kimi', 'https://api.moonshot.cn/v1', '',
        '[{"id":"kimi-k2.6","name":"kimi-k2.6","object":"model","owned_by":"moonshot","capabilities":["function_calling","vision"]},{"id":"kimi-k2.5","name":"kimi-k2.5","object":"model","owned_by":"moonshot","capabilities":["function_calling","vision"]},{"id":"kimi-k2","name":"kimi-k2","object":"model","owned_by":"moonshot","capabilities":["function_calling"]},{"id":"kimi-k2-thinking","name":"kimi-k2-thinking","object":"model","owned_by":"moonshot","capabilities":["function_calling","reasoning"]}]',
        1, 1, 10);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (11, 'doubao', '火山引擎豆包', 'https://ark.cn-beijing.volces.com/api/v3', '',
        '[{"id":"doubao-seed-2.0-pro","name":"doubao-seed-2.0-pro","object":"model","owned_by":"doubao","capabilities":["function_calling","vision"]},{"id":"doubao-seed-2.0-lite","name":"doubao-seed-2.0-lite","object":"model","owned_by":"doubao","capabilities":["function_calling"]},{"id":"doubao-seed-2.0-mini","name":"doubao-seed-2.0-mini","object":"model","owned_by":"doubao","capabilities":["function_calling"]},{"id":"doubao-seed-2.0-code","name":"doubao-seed-2.0-code","object":"model","owned_by":"doubao","capabilities":["function_calling"]},{"id":"doubao-pro","name":"doubao-pro","object":"model","owned_by":"doubao","capabilities":["function_calling"]},{"id":"doubao-lite","name":"doubao-lite","object":"model","owned_by":"doubao","capabilities":["function_calling"]},{"id":"doubao-vision","name":"doubao-vision","object":"model","owned_by":"doubao","capabilities":["function_calling","vision"]}]',
        1, 1, 11);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (12, 'hunyuan', '腾讯混元', 'https://api.hunyuan.cloud.tencent.com/v1', '',
        '[{"id":"hunyuan-pro","name":"hunyuan-pro","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]},{"id":"hunyuan-standard","name":"hunyuan-standard","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]},{"id":"hunyuan-lite","name":"hunyuan-lite","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]},{"id":"hunyuan-code","name":"hunyuan-code","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]},{"id":"hunyuan-vision","name":"hunyuan-vision","object":"model","owned_by":"hunyuan","capabilities":["function_calling","vision"]},{"id":"hunyuan-functioncall","name":"hunyuan-functioncall","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]},{"id":"hunyuan-role","name":"hunyuan-role","object":"model","owned_by":"hunyuan","capabilities":["function_calling"]}]',
        1, 1, 12);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (13, 'ernie', '百度文心', 'https://aip.baidubce.com', '',
        '[{"id":"ERNIE-4.5","name":"ERNIE-4.5","object":"model","owned_by":"ernie","capabilities":["function_calling","vision"]},{"id":"ERNIE-4.0-Turbo","name":"ERNIE-4.0-Turbo","object":"model","owned_by":"ernie","capabilities":["function_calling"]},{"id":"ERNIE-3.5","name":"ERNIE-3.5","object":"model","owned_by":"ernie","capabilities":["function_calling"]},{"id":"ERNIE-Speed","name":"ERNIE-Speed","object":"model","owned_by":"ernie","capabilities":["function_calling"]},{"id":"ERNIE-Lite","name":"ERNIE-Lite","object":"model","owned_by":"ernie","capabilities":["function_calling"]},{"id":"ERNIE-Tiny","name":"ERNIE-Tiny","object":"model","owned_by":"ernie","capabilities":["function_calling"]}]',
        1, 1, 13);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (14, 'lingyi', '零一万物', 'https://api.lingyiwanwu.com/v1', '',
        '[{"id":"yi-lightning","name":"yi-lightning","object":"model","owned_by":"lingyi","capabilities":["function_calling"]},{"id":"yi-medium","name":"yi-medium","object":"model","owned_by":"lingyi","capabilities":["function_calling"]},{"id":"yi-large","name":"yi-large","object":"model","owned_by":"lingyi","capabilities":["function_calling"]},{"id":"yi-large-turbo","name":"yi-large-turbo","object":"model","owned_by":"lingyi","capabilities":["function_calling"]},{"id":"yi-large-vision","name":"yi-large-vision","object":"model","owned_by":"lingyi","capabilities":["function_calling","vision"]},{"id":"yi-medium-200k","name":"yi-medium-200k","object":"model","owned_by":"lingyi","capabilities":["function_calling"]},{"id":"yi-spark","name":"yi-spark","object":"model","owned_by":"lingyi","capabilities":["function_calling"]}]',
        1, 1, 14);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (15, 'baichuan', '百川智能', 'https://api.baichuan-ai.com/v1', '',
        '[{"id":"Baichuan4-Turbo","name":"Baichuan4-Turbo","object":"model","owned_by":"baichuan","capabilities":["function_calling"]},{"id":"Baichuan4-Air","name":"Baichuan4-Air","object":"model","owned_by":"baichuan","capabilities":["function_calling"]},{"id":"Baichuan3-Turbo","name":"Baichuan3-Turbo","object":"model","owned_by":"baichuan","capabilities":["function_calling"]},{"id":"Baichuan3-Turbo-128k","name":"Baichuan3-Turbo-128k","object":"model","owned_by":"baichuan","capabilities":["function_calling"]}]',
        1, 1, 15);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (16, 'spark', '讯飞星火', 'https://spark-api.xf-yun.com/v3.5', '',
        '[{"id":"spark-4.0","name":"spark-4.0","object":"model","owned_by":"spark","capabilities":["function_calling"]},{"id":"spark-3.5","name":"spark-3.5","object":"model","owned_by":"spark","capabilities":["function_calling"]},{"id":"spark-3.0","name":"spark-3.0","object":"model","owned_by":"spark","capabilities":["function_calling"]},{"id":"spark-pro","name":"spark-pro","object":"model","owned_by":"spark","capabilities":["function_calling"]},{"id":"spark-lite","name":"spark-lite","object":"model","owned_by":"spark","capabilities":["function_calling"]},{"id":"spark-vision","name":"spark-vision","object":"model","owned_by":"spark","capabilities":["function_calling","vision"]}]',
        1, 1, 16);
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order)
VALUES (17, 'minimax', 'MiniMax', 'https://api.minimax.chat/v1', '',
        '[{"id":"MiniMax-Text-01","name":"MiniMax-Text-01","object":"model","owned_by":"minimax","capabilities":["function_calling"]},{"id":"MiniMax-T2S","name":"MiniMax-T2S","object":"model","owned_by":"minimax","capabilities":["function_calling"]}]',
        1, 1, 17);


