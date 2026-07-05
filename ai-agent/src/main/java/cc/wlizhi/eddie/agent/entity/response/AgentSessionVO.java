/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体会话列表响应 VO
 */
@Getter
@Setter
public class AgentSessionVO {

    /**
     * 会话 ID
     */
    private Long id;

    /**
     * 归属智能体 ID
     */
    private Long agentId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 0=普通, 1=置顶
     */
    private Integer pinned;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 累计 token 数
     */
    private Integer totalTokens;

    /**
     * 最后活跃时间
     */
    private Long updatedAt;
}
