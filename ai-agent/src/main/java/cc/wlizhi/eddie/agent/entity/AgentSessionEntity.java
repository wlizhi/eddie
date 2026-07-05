/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体会话 — 映射 ai_agent_session 表
 */
@Setter
@Getter
public class AgentSessionEntity {

    private Long id;
    private Long agentId;
    private String title;
    private Integer pinned;
    private Integer messageCount;
    private Integer totalTokens;
    private Long createdAt;
    private Long updatedAt;
}
