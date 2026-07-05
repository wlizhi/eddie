/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体消息记录响应 VO
 * <p>
 * 对应 ai_agent_session_msg 表，返回给前端展示。
 */
@Getter
@Setter
public class AgentMessageVO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 归属会话 ID
     */
    private Long sessionId;

    /**
     * 归属智能体 ID
     */
    private Long agentId;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 角色：user / assistant / system
     */
    private String role;

    /**
     * 供应商实例 ID
     */
    private Long providerId;

    /**
     * 模型 code
     */
    private String modelCode;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 思考内容
     */
    private String thinking;

    /**
     * 回答内容（精简摘要）
     */
    private String content;

    /**
     * prompt token 数
     */
    private Integer promptTokens;

    /**
     * completion token 数
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;

    /**
     * 预估费用
     */
    private Double priceEstimate;

    /**
     * 币种
     */
    private String currency;

    /**
     * 工具调用 JSON
     */
    private String toolCalls;

    /**
     * 缓存读取 input token 数
     */
    private Integer cacheReadInputTokens;

    /**
     * 缓存写入 input token 数
     */
    private Integer cacheWriteInputTokens;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 消息状态：COMPLETED / STOPPED / ERROR
     */
    private String msgStatus;

    /**
     * 创建时间
     */
    private Long createdAt;
}
