/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.response.AgentMessageVO;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.agent.service.AgentSessionService;
import cc.wlizhi.eddie.common.dto.PageResult;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体会话管理业务实现
 */
@Service
public class AgentSessionServiceImpl implements AgentSessionService {

    @Resource
    private AgentSessionDao agentSessionDao;

    @Resource
    private AgentMsgDao agentMsgDao;

    @Override
    public AgentSessionVO create(Long agentId) {
        AgentSessionEntity entity = new AgentSessionEntity();
        entity.setAgentId(agentId);
        agentSessionDao.insert(entity);
        Long id = agentSessionDao.findLastInsertId();
        AgentSessionEntity saved = agentSessionDao.findById(id);
        return toVO(saved);
    }

    @Override
    public PageResult<AgentSessionVO> list(Long agentId, String title, int pageNum, int pageSize) {
        long total = agentSessionDao.countByAgentId(agentId, title);
        if (total == 0) {
            return PageResult.empty();
        }
        int offset = (pageNum - 1) * pageSize;
        List<AgentSessionEntity> entities = agentSessionDao.findByAgentIdPaged(agentId, title, offset, pageSize);

        List<AgentSessionVO> vos = new ArrayList<>();
        for (AgentSessionEntity entity : entities) {
            vos.add(toVO(entity));
        }
        return PageResult.of(pageNum, pageSize, total, vos);
    }

    @Override
    public void delete(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentMsgDao.deleteBySessionId(id);
        agentSessionDao.deleteById(id);
    }

    @Override
    public AgentSessionVO renameTitle(Long id, String title) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updateTitle(id, title);
        return toVO(agentSessionDao.findById(id));
    }

    @Override
    public void pin(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updatePinned(id, 1);
    }

    @Override
    public void unpin(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updatePinned(id, 0);
    }

    @Override
    public List<AgentMessageVO> findMessagesBySessionId(Long sessionId, Long beforeId, int limit) {
        List<AgentMsgEntity> entities = agentMsgDao.findBySessionId(sessionId, beforeId, limit);
        // AgentMsgDao 返回倒序（最新在前），反转为正序
        List<AgentMessageVO> vos = new ArrayList<>(entities.size());
        for (int i = entities.size() - 1; i >= 0; i--) {
            vos.add(toMessageVO(entities.get(i)));
        }
        return vos;
    }

    // ==================== 内部方法 ====================

    private AgentSessionVO toVO(AgentSessionEntity entity) {
        AgentSessionVO vo = new AgentSessionVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setTitle(entity.getTitle());
        vo.setPinned(entity.getPinned());
        vo.setMessageCount(entity.getMessageCount() != null ? entity.getMessageCount() : 0);
        vo.setTotalTokens(entity.getTotalTokens() != null ? entity.getTotalTokens() : 0);
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AgentMessageVO toMessageVO(AgentMsgEntity entity) {
        AgentMessageVO vo = new AgentMessageVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setAgentId(entity.getAgentId());
        vo.setTaskId(entity.getTaskId());
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
        vo.setCurrency(entity.getCurrency());
        vo.setToolCalls(entity.getToolCalls());
        vo.setCacheReadInputTokens(entity.getCacheReadInputTokens());
        vo.setCacheWriteInputTokens(entity.getCacheWriteInputTokens());
        vo.setDurationMs(entity.getDurationMs());
        vo.setMsgStatus(entity.getMsgStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
