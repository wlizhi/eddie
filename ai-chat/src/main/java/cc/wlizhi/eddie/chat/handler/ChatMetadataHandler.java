/**
 * ChatMetadataHandler — 响应元数据处理器
 * <p>
 * 扩展点：在流式响应结束后构建元数据（耗时、Token 用量等）。
 * 不同服务商的 Usage 字段结构和命名可能不同：
 * - OpenAI: prompt_tokens, completion_tokens, total_tokens
 * - DeepSeek: 同 OpenAI
 * - Anthropic: input_tokens, output_tokens
 * <p>
 * 新增服务商时只需实现此接口，注册为 Spring Bean 即可。
 */
package cc.wlizhi.eddie.chat.handler;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;

import java.util.Map;

/**
 * 元数据处理接口
 */
public interface ChatMetadataHandler {

    /**
     * 判断是否支持处理该服务商的元数据
     */
    boolean support(String providerCode);

    /**
     * 构建元数据 Map
     *
     * @param ctx 上下文（含 startTime, lastResponse 等）
     * @return 元数据 Map，将被序列化为 JSON 发送给前端
     */
    Map<String, Object> buildMetadata(ChatContext ctx);
}
