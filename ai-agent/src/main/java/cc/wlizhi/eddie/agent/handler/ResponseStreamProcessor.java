package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 响应流处理器 — 策略接口
 * <p>
 * 每种 {@link AgentMode} 对应一个实现，负责处理该模式下的 ChatResponse 流，
 * 包括思考内容提取、回答文本提取、模式特有事件推送等。
 *
 * @author Eddie
 * {@code @date} 2026-07-06
 */
public interface ResponseStreamProcessor {

    /**
     * 当前处理器是否支持该模式
     */
    boolean support(AgentMode agentMode);

    /**
     * 处理 ChatResponse 流，向前端推送 SSE 事件
     *
     * @param ctx         当前请求上下文
     * @param requestSpec 已构建好的 ChatClient 请求体
     */
    void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec);
}
