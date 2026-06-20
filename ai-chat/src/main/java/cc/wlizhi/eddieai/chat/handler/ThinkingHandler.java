/**
 * ThinkingHandler — 思考内容处理器
 * <p>
 * 扩展点：在流式响应中提取和处理模型的"思考过程"内容。
 * 不同服务商对思考内容的字段命名和格式不同：
 * - DeepSeek: reasoning_content 字段
 * - OpenAI: 无思考内容
 * - Anthropic: 可能在不同位置
 * <p>
 * 新增服务商时只需实现此接口，注册为 Spring Bean 即可。
 */
package cc.wlizhi.eddieai.chat.handler;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;

/**
 * 思考内容处理接口
 */
public interface ThinkingHandler {

    /**
     * 判断是否支持处理该服务商的思考内容
     */
    boolean support(String providerCode);

    /**
     * 从 ChatResponse 中提取思考内容
     *
     * @param rawThinking 原始思考内容
     * @param ctx         上下文
     * @return 处理后的思考内容，无需处理则返回原值
     */
    String process(String rawThinking, ChatContext ctx);
}
