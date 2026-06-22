package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.dao.MessageDao;
import cc.wlizhi.eddieai.chat.dao.SessionDao;
import cc.wlizhi.eddieai.chat.entity.MessageEntity;
import cc.wlizhi.eddieai.chat.entity.SessionEntity;
import cc.wlizhi.eddieai.chat.entity.response.MessageVO;
import cc.wlizhi.eddieai.chat.entity.response.SessionVO;
import cc.wlizhi.eddieai.chat.service.SessionService;
import cc.wlizhi.eddieai.common.dto.PageResult;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.common.exception.NotFoundException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话管理业务实现
 */
@Service
public class SessionServiceImpl implements SessionService {

    @Resource
    private SessionDao sessionDao;

    @Resource
    private MessageDao messageDao;

    private static final int MESSAGE_PAGE_SIZE = 20;

    @Override
    public SessionVO create(Long assistantId) {
        SessionEntity entity = new SessionEntity();
        entity.setAssistantId(assistantId);
        sessionDao.insert(entity);
        Long id = sessionDao.findLastInsertId();
        SessionEntity saved = sessionDao.findById(id);
        return toVO(saved);
    }

    @Override
    public PageResult<SessionVO> list(Long assistantId, String title, int pageNum, int pageSize) {
        long total = sessionDao.countByAssistantId(assistantId, title);
        int offset = (pageNum - 1) * pageSize;
        List<SessionEntity> entities = sessionDao.findByAssistantIdPaged(assistantId, title, offset, pageSize);

        List<SessionVO> vos = new ArrayList<>();
        for (SessionEntity entity : entities) {
            vos.add(toVO(entity));
        }
        return PageResult.of(pageNum, pageSize, total, vos);
    }

    @Override
    public void delete(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        messageDao.deleteBySessionId(id);
        sessionDao.deleteById(id);
    }

    @Override
    public SessionVO renameTitle(Long id, String title) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updateTitle(id, title);
        return toVO(sessionDao.findById(id));
    }

    @Override
    public void pin(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updatePinned(id, 1);
    }

    @Override
    public void unpin(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updatePinned(id, 0);
    }

    @Override
    public String generateTitle(Long sessionId, Long providerId, String modelCode) {
        if (!sessionDao.existsById(sessionId)) {
            throw new NotFoundException("会话不存在: " + sessionId);
        }

        List<MessageEntity> firstRound = messageDao.findFirstRound(sessionId);
        if (firstRound.size() < 2) {
            throw new BadRequestException("会话消息不足，无法生成标题");
        }

        String userMsg = "";
        String assistantMsg = "";
        for (MessageEntity msg : firstRound) {
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole())) {
                assistantMsg = msg.getContent();
            }
        }

        // TODO: 调用模型生成标题（使用 providerId + modelCode 创建 ChatClient）
        // 当前简化实现：取用户首条消息前 20 字作为标题
        String title = userMsg.length() > 20 ? userMsg.substring(0, 20) : userMsg;
        sessionDao.updateTitle(sessionId, title);
        return title;
    }

    @Override
    public List<MessageVO> getMessages(Long sessionId, Long beforeId) {
        if (!sessionDao.existsById(sessionId)) {
            throw new NotFoundException("会话不存在: " + sessionId);
        }
        List<MessageEntity> entities = messageDao.findBySessionId(sessionId, beforeId, MESSAGE_PAGE_SIZE);
        List<MessageVO> result = new ArrayList<>();
        for (MessageEntity entity : entities) {
            result.add(toMessageVO(entity));
        }
        return result;
    }

    // ==================== 内部方法 ====================

    private SessionVO toVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        vo.setId(entity.getId());
        vo.setAssistantId(entity.getAssistantId());
        vo.setTitle(entity.getTitle());
        vo.setPinned(entity.getPinned());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setMessageCount(entity.getMessageCount() != null ? entity.getMessageCount() : 0);
        return vo;
    }

    private MessageVO toMessageVO(MessageEntity entity) {
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
        vo.setRole(entity.getRole());
        vo.setProviderId(entity.getProviderId());
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setThinking(entity.getThinking());
        vo.setContent(entity.getContent());
        vo.setPromptTokens(entity.getPromptTokens());
        vo.setCompletionTokens(entity.getCompletionTokens());
        vo.setTotalTokens(entity.getTotalTokens());
        vo.setPriceEstimate(entity.getPriceEstimate());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
