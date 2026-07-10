/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.context.AgentContext;
import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.memory.shortterm.AbstractWindowedMemory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Resource
    private AgentMsgDao agentMsgDao;

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

    @Override
    @NonNull
    protected List<Message> loadHistory(String conversationId, int maxRounds) {
        Long sessionId = Long.parseLong(conversationId);
        // 只查 round_seq > 0 的消息（跳过 round_seq=0 的占位消息），取最新 maxRounds*2 条
        int limit = maxRounds * 2;
        List<AgentMsgEntity> messages = agentMsgDao.findBySessionIdCompleted(sessionId, null, limit);
        if (messages.isEmpty()) {
            return List.of();
        }
        // findBySessionIdCompleted 返回倒序（最新在前），翻转成正序
        Collections.reverse(messages);
        List<Message> result = new ArrayList<>(messages.size());
        for (AgentMsgEntity msg : messages) {
            String content = msg.getContent() != null ? msg.getContent() : "";
            if ("user".equals(msg.getRole())) {
                result.add(new UserMessage(content));
            } else if ("assistant".equals(msg.getRole())) {
                result.add(new AssistantMessage(content));
            }
        }
        return result;
    }
}
