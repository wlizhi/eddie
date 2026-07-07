/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

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

    private Thread agentThread;

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
    private FluxSink<ServerSentEvent<String>> sink;

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

    /**
     * 迭代状态
     */
    AgentIteratorState iteratorState;

    /**
     * JSON 序列化器 — 供 AgentToolCallbackWrapper 等组件使用
     */
    private ObjectMapper objectMapper;

    /**
     * 事件发射器
     */
    private AgentEventPublisher eventPublisher;

    /**
     * 事件注册表（用于跨线程停止检测）
     */
    private EventRegistry eventRegistry;

    // ==================== 规划上下文 ====================

    /**
     * 任务规划清单（PLAN 模式由模型输出，供后续执行步骤使用）
     */
    private AgentTaskPlan taskPlan;

    // ==================== 执行上下文 =====================
    /**
     * 步骤列表，写时隔离，每个线程读取置顶索引值
     */
    private List<List<AgentMsgStepEntity>> taskStepList;

    /**
     * 当前步骤序号，从1开始，用来表示当前正在执行的步骤
     */
    private Integer currentStep;

    /**
     * 步骤级流式累加器（执行模式专用），
     * 包含 stepId、独立于消息级别的 thinking/answer/toolCalls 累加。
     */
    private AgentStepStreamContext stepStreamContext;

    // ==================== 流式累加（用于最终持久化） ====================
    /**
     * 流式处理中累积的回答内容
     */
    private final StringBuilder fullAnswer = new StringBuilder();

    /**
     * 流式处理中累积的思考内容（reasoning_content）
     */
    private final StringBuilder fullThinking = new StringBuilder();

    /**
     * 工具执行记录列表（用于持久化到 ai_agent_session_msg.tool_calls）
     */
    private final List<ToolExecutionEvent> toolCalls = new ArrayList<>();

    /**
     * 工具结果返回模型前的最大字符数（0=不截断），来自 TOOL_RESULT_MODEL_MAX_LENGTH 配置
     */
    private int toolResultModelMaxLength = 100000;

    /**
     * 工具结果 SSE 渲染的最大字符数（0=不截断），来自 TOOL_CALL_MAX_LENGTH 配置
     */
    private int toolCallMaxLength = 5000;

    /**
     * 工具结果持久化到数据库的最大字符数（0=不截断）
     */
    private int toolCallStoreMaxLength = 4000;
}
