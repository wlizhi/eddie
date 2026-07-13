/**
 * ChatMetadataHandler — 响应元数据处理器
 * <p>
 * 扩展点：在流式响应结束后构建元数据（耗时、Token 用量等）。
 * 返回 MetadataInfo 实体并存入 ChatContext，作为单一数据源
 * 同时供 SSE 事件推送和消息持久化使用。
 * <p>
 * 不同服务商的 Usage 字段结构和命名可能不同：
 * - OpenAI: prompt_tokens, completion_tokens, total_tokens
 * - DeepSeek: 同 OpenAI
 * - Anthropic: input_tokens, output_tokens
 * <p>
 * 新增服务商时只需实现此接口，注册为 Spring Bean 即可。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.handler;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatMetadataInfoPayload;

/**
 * 元数据处理接口
 */
public interface ChatMetadataHandler {

    /**
     * 判断是否支持处理该服务商的元数据
     */
    boolean support(String providerCode);

    /**
     * 构建元数据，并存入 ChatContext
     *
     * @param ctx 上下文（含 startTime, lastResponse 等）
     * @return 元数据实体，将被序列化为 JSON 发送给前端并用于持久化
     */
    ChatMetadataInfoPayload buildMetadata(ChatContext ctx);
}
