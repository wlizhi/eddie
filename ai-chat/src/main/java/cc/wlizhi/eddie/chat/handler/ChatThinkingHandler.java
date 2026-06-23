/**
 * ChatThinkingHandler — 思考内容处理器
 * <p>
 * 扩展点：在流式响应中提取和处理模型的"思考过程"内容。
 * 不同服务商对思考内容的字段命名和格式不同：
 * - DeepSeek: reasoning_content 字段
 * - OpenAI: 无思考内容
 * - Anthropic: 可能在不同位置
 * <p>
 * 新增服务商时只需实现此接口，注册为 Spring Bean 即可。
 */
package cc.wlizhi.eddie.chat.handler;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 思考内容处理接口
 * <p>
 * 职责：从 ChatResponse 中提取并处理模型的"思考过程"内容。
 * 提取和处理逻辑本身就是 provider-specific 的，因此 Handler 同时负责两者。
 */
public interface ChatThinkingHandler {

    /**
     * 判断是否支持处理该服务商的思考内容
     */
    boolean support(String providerCode);

    /**
     * 从 ChatResponse 中提取并处理思考内容
     *
     * @param response ChatResponse（包含完整的 generation 输出）
     * @param ctx      上下文
     * @return 处理后的思考内容字符串，无思考内容则返回 null
     */
    String extractThinking(ChatResponse response, ChatContext ctx);
}
