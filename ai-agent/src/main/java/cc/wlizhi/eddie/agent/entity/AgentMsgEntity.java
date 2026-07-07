/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体消息记录 — 映射 ai_agent_session_msg 表
 * <p>
 * content 存精简摘要文本（前端对话气泡展示），
 * 完整执行过程（多轮次/工具调用）存 AgentMsgStepEntity。
 */
@Setter
@Getter
public class AgentMsgEntity {

    private Long id;
    private Long sessionId;
    private Long agentId;
    private String role;
    private Long providerId;
    private String modelCode;
    private String modelName;
    private String thinking;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Double priceEstimate;
    private String toolCalls;
    private String taskPlan;
    private Integer cacheReadInputTokens;
    private Integer cacheWriteInputTokens;
    private String currency;
    private Integer durationMs;
    private String msgStatus;
    private Long createdAt;
}
