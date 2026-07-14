/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型回复上下文 — 存放模型输出的回复内容、工具调用记录及相关截断配置。
 *
 * @author Eddie
 * {@code @date} 2026-07-13
 */
@Getter
@Setter
public class AgentOutputContext {

    /**
     * 工具回调列表
     */
    private ToolCallback[] toolCallbacks;

    /**
     * 流式处理中累积的回答内容
     */
    private StringBuilder fullAnswer = new StringBuilder();

    /**
     * 流式处理中累积的思考内容（reasoning_content）
     */
    private StringBuilder fullThinking = new StringBuilder();

    /**
     * 工具执行记录列表（用于持久化到 ai_agent_session_msg.tool_calls）
     */
    private List<ChatToolExecutionEvent> toolCalls = new ArrayList<>();

    /**
     * 消耗统计
     */
    private AgentTokenStatists tokenStatists;

    /**
     * 工具调用序号计数器（从 1 开始自动递增），
     * 用于构建唯一审批 key，区分同一轮对话中同一工具的多次调用。
     * <p>
     * CHAT 模式使用此计数器（EXECUTE 模式使用 stepStreamContext 中的计数器）。
     */
    private final AtomicInteger toolCallSequence = new AtomicInteger(0);

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
