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

@Getter
@Setter
public class AgentChatContext {

    // ==================== 阶段一：原始请求 ====================
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

    // ==================== 迭代状态 ====================

    /**
     * 迭代状态
     */
    private final AgentIteratorState iteratorState = new AgentIteratorState();

    // ==================== 规划上下文 ====================

    /**
     * 任务规划清单（PLAN 模式由模型输出，供后续执行步骤使用）
     */
    private AgentTaskPlan taskPlan;

    // ==================== 执行上下文 =====================

    /**
     * 步骤级流式累加器（执行模式专用），作用域是每一步的迭代中
     * 包含 stepId、独立于消息级别的 thinking/answer/toolCalls 累加。
     */
    private AgentStepStreamContext stepStreamContext;

    // ==================== 组合上下文 ====================

    /**
     * 模型回复上下文 — 模型输出的回复内容、工具调用记录、截断配置等
     */
    private final AgentOutputContext output = new AgentOutputContext();

    /**
     * 指标/计数上下文 — 请求时间、步骤计数等
     */
    private final AgentMetrics metrics = new AgentMetrics();

    /**
     * 事件/功能上下文 — SSE 推送、事件发布、线程、序列化等基础设施
     */
    private final AgentEventContext event = new AgentEventContext();
}
