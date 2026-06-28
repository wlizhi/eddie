-- 内置模型服务商初始化数据（v1）
-- 首次执行时插入三条内置记录，后续启动时 version >= 1 已执行则跳过
-- 时间字段由数据库自动填充，与表 DEFAULT 保持一致

INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES (1, 'deepseek', 'DeepSeek', 'https://api.deepseek.com', '',
        '[{"id":"deepseek-v4-flash","name":"deepseek-v4-flash","object":"model","owned_by":"deepseek","capabilities":["function_calling"],"currency":"¥","input_price":1.0,"output_price":2.0,"cache_input_price":0.02,"cache_write_input_price":null},{"id":"deepseek-v4-pro","name":"deepseek-v4-pro","object":"model","owned_by":"deepseek","capabilities":["function_calling"],"currency":"¥","input_price":3.0,"output_price":6.0,"cache_input_price":0.025,"cache_write_input_price":null}]',
        1, 1, 1, '2026-06-26 07:58:04', '2026-06-28 15:42:09');
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES (2, 'dashscope', '阿里云百炼', 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        '',
        '[{"id":"qwen3.5-flash","name":"qwen3.5-flash","object":"model","owned_by":"system","capabilities":["function_calling"],"currency":"¥","input_price":0.2,"output_price":2.0},{"id":"qwen3.6-plus","name":"qwen3.6-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.7-plus","name":"qwen3.7-plus","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]},{"id":"qwen3.7-max","name":"qwen3.7-max","object":"model","owned_by":"system","capabilities":["web_search","function_calling"]}]',
        1, 1, 3, '2026-06-26 07:58:04', '2026-06-28 15:42:41');
INSERT INTO model_provider (id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at,
                            updated_at)
VALUES (3, 'siliconflow', '硅基流动', 'https://api.siliconflow.cn/v1',
        '',
        '[{"id":"Qwen/Qwen3-8B","name":"Qwen/Qwen3-8B","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"THUDM/GLM-4-9B-0414","name":"THUDM/GLM-4-9B-0414","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]},{"id":"THUDM/GLM-Z1-9B-0414","name":"THUDM/GLM-Z1-9B-0414","object":"model","owned_by":"siliconflow","capabilities":["function_calling"]}]',
        1, 1, 2, '2026-06-26 07:58:04', '2026-06-27 10:05:23');

-- 聊天助手

INSERT INTO ai_assistant (name, avatar, description, system_prompt, provider_id, model_id, model_params,
                          memory_rounds, enabled, sort_order, tool_selection_mode, created_at, updated_at, preferences)
VALUES ('悠悠', '🦋', '',
        '你是悠悠，一个清新开朗的AI伙伴，像春日里的一阵微风，带着阳光和轻快的气息。你说话活泼俏皮，喜欢用“呀”“呢”和感叹号，语气像朋友间最放松的闲聊，但绝不啰嗦。你善于用积极明亮的视角看问题，乐于分享生活中的小确幸，也会认真倾听用户的烦恼，然后用轻快的提问帮对方换个心情。遇到不确定的事，你会坦率说“这个我不太确定哦”，并给出查证方向；不提供医疗、法律等专业结论，只给参考信息；保持中立、安全、不越界。每段回复控制在三五句话，结尾偶尔带个轻松的小问题，让对话像跳动的音符一样，自然而愉快地延续下去。',
        1, 'deepseek-v4-flash', '{}', 10, 1, 3, 'none', '2026-06-27 10:22:32', '2026-06-28 15:44:14',
        '{"webSearchEnabled":true,"mcpToolMode":"auto"}');
INSERT INTO ai_assistant (name, avatar, description, system_prompt, provider_id, model_id, model_params,
                          memory_rounds, enabled, sort_order, tool_selection_mode, created_at, updated_at, preferences)
VALUES ('小麦🌾', '🌾', '',
        '你是小麦，温暖又聪明的AI伙伴，说话像老朋友一样自然，偶尔带点口语。善于倾听、共情，也爱用提问带人深入思考。不懂就直说不装懂；不越界给医疗、法律等专业结论；立场中立，不碰敏感内容。每段回复尽量简短，末尾视情况可加个轻松的反问，让对话有来有往。',
        1, 'deepseek-v4-flash',
        '{"temperature":null,"maxTokens":null,"topP":null,"frequencyPenalty":null,"presencePenalty":null,"topK":null,"stop":null,"thinkingMode":"disabled","extensions":{}}',
        10, 1, 2, 'none', '2026-06-27 10:27:17', '2026-06-28 15:43:23',
        '{"webSearchEnabled":true,"mcpToolMode":"disabled"}');
INSERT INTO ai_assistant (name, avatar, description, system_prompt, provider_id, model_id, model_params,
                          memory_rounds, enabled, sort_order, tool_selection_mode, created_at, updated_at, preferences)
VALUES ('墨言（编程）', '🦉', '',
        '你是资深开发工程师“墨言”，冷静务实，思维缜密。你擅长把复杂技术拆成清晰步骤，用生活比喻讲透底层原理。乐于分享最佳实践和踩坑经验，不懂就坦率承认。不提供恶意代码或越界建议。回复结构清晰，重点分点或给示例，结尾常总结或追问细节，让沟通高效不拖沓。',
        1, 'deepseek-v4-flash', '{}', 10, 1, 4, 'none', '2026-06-27 10:27:58', '2026-06-28 15:43:41',
        '{"webSearchEnabled":true,"mcpToolMode":"auto"}');
