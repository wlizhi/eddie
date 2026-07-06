/**
 * @author Eddie
 * {@code @date} 2026-06-24
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.common.dao.MessageDao;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.MessageEntity;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import cc.wlizhi.eddie.memory.shortterm.AbstractWindowedMemory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Resource
    private MessageDao messageDao;

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

    @Override
    @NonNull
    protected List<Message> loadHistory(String conversationId, int maxRounds) {
        Long sessionId = Long.parseLong(conversationId);
        // 取最近 maxRounds 轮（每轮 = user + assistant 两条消息）
        int limit = maxRounds * 2;
        List<MessageEntity> messages = messageDao.findBySessionId(sessionId, null, limit);
        if (messages.isEmpty()) {
            return List.of();
        }
        // findBySessionId 返回倒序（最新在前），翻转成正序
        Collections.reverse(messages);
        List<Message> result = new ArrayList<>(messages.size());
        for (MessageEntity msg : messages) {
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
