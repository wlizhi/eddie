/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * 事件/功能上下文 — 存放 SSE 推送、事件发布、线程、JSON 序列化等基础设施。
 *
 * @author Eddie
 * {@code @date} 2026-07-13
 */
@Getter
@Setter
public class AgentEventContext {

    private Thread agentThread;

    /**
     * SSE 事件 Sink — 虚拟线程通过此向前端推送事件
     */
    private FluxSink<ServerSentEvent<String>> sink;

    /**
     * 事件发射器
     */
    private AgentEventPublisher eventPublisher;

    /**
     * 事件注册表（用于跨线程停止检测）
     */
    private EventRegistry eventRegistry;

    /**
     * 构建好的 ChatClient
     */
    private ChatClient chatClient;

    /**
     * JSON 序列化器 — 供 AgentToolCallbackWrapper 等组件使用
     */
    private ObjectMapper objectMapper;

    /**
     * 工具调用错误
     */
    private StringBuilder toolErrorFeedback = new StringBuilder();
}
