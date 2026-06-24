package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import cc.wlizhi.eddie.memory.shortterm.AbstractWindowedMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 助手短期记忆实现 — 从助手配置中读取 memoryRounds
 * <p>
 * 通过 {@link SessionDao} 查询会话 → 获取 {@link AssistantEntity#getMemoryRounds()}，
 * 实现每个助手可独立配置记忆轮数的能力。
 */
@Service
public class AssistantShortTermMemory extends AbstractWindowedMemory {

    @Resource
    private SessionDao sessionDao;

    @Resource
    private AssistantContext assistantContext;

    @Override
    protected int resolveMaxRounds(String conversationId) {
        Long sessionId = Long.parseLong(conversationId);
        SessionEntity session = sessionDao.findById(sessionId);
        if (session == null) {
            return 0;
        }
        AssistantEntity assistant = assistantContext.getAssistantById(session.getAssistantId());
        if (assistant == null || assistant.getMemoryRounds() == null) {
            return 0;
        }
        // 0 = 无记忆，>0 = 保留 N 轮
        return assistant.getMemoryRounds();
    }
}
