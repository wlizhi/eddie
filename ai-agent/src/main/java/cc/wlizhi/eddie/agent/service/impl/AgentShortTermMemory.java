/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.context.AgentContext;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.memory.shortterm.AbstractWindowedMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 智能体短期记忆实现 — 从智能体配置中读取 memoryRounds
 * <p>
 * 通过 {@link AgentSessionDao} 查询会话 → 获取 {@link AgentEntity#getMemoryRounds()}，
 * 实现每个智能体可独立配置记忆轮数的能力。
 */
@Component
public class AgentShortTermMemory extends AbstractWindowedMemory {

    @Resource
    private AgentSessionDao agentSessionDao;

    @Resource
    private AgentContext agentContext;

    @Override
    protected int resolveMaxRounds(String conversationId) {
        Long sessionId = Long.parseLong(conversationId);
        AgentSessionEntity session = agentSessionDao.findById(sessionId);
        if (session == null) {
            return 0;
        }
        AgentEntity agent = agentContext.getAgentById(session.getAgentId());
        if (agent == null || agent.getMemoryRounds() == null) {
            return 0;
        }
        // 0 = 无记忆，>0 = 保留 N 轮
        return agent.getMemoryRounds();
    }
}
