/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

@Getter
@Setter
public class AgentChatContext {

    // ==================== 阶段一：原始请求 ====================
    /**
     * 请求开始时间戳
     */
    private long startTime;

    /**
     * 用户原始请求
     */
    private AgentChatRequest originalRequest;

    // ==================== 阶段二：预处理，必要信息填充 ====================

    /**
     * 智能体实体
     */
    private AgentEntity agent;

    /**
     * 智能体会话
     */
    private AgentSessionEntity session;

    /**
     * 主模型服务商
     */
    private ModelProviderEntity modelProvider;

    /**
     * 最终使用的模型信息
     */
    private AgentModelInfo useModelInfo;

    /**
     * 持久化数据库的用户消息
     */
    private AgentMsgEntity userMsg;

    /**
     * 当前对话智能体的回复消息（用户可见的）
     */
    private AgentMsgEntity agentMsg;

    // ==================== SSE 推送 ====================

    /**
     * SSE 事件 Sink — 虚拟线程通过此向前端推送事件
     */
    private Sinks.Many<ServerSentEvent<String>> sink;

    // ==================== 模型上下文 ====================

    /**
     * 构建好的 ChatClient
     */
    private ChatClient chatClient;

    /**
     * 工具回调列表
     */
    private ToolCallback[] toolCallbacks;

    /**
     * 最后一次 ChatResponse（用于提取 tool_calls 和 token 用量）
     */
    private ChatResponse lastResponse;

    /**
     * 消耗统计
     */
    private AgentTokenStatists tokenStatists;
}
