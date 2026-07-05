/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.context;

import cc.wlizhi.eddie.agent.dao.AgentDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Agent 智能体上下文，全应用生命周期缓存
 * <p>
 * 以 {@code agentId} 为键快速查找智能体信息，包括系统提示词、模型参数等。
 * 参考 {@code AssistantContext} 实现。
 */
@Component
public class AgentContext implements GlobalCache {

    private volatile Map<Long, AgentEntity> agentMap;
    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private AgentDao agentDao;
    @Resource
    private InitScheduler initScheduler;

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1000, this::refresh);
    }

    /**
     * 根据 agentId 精确获取智能体信息
     */
    public AgentEntity getAgentById(Long agentId) {
        AgentEntity entity = agentMap.get(agentId);
        if (entity != null) {
            return entity;
        }
        refresh();
        return agentMap.get(agentId);
    }

    @Override
    public void refresh() {
        try {
            lock.lock();
            List<AgentEntity> all = agentDao.findAll(true);
            Map<Long, AgentEntity> map = new LinkedHashMap<>();
            for (AgentEntity entity : all) {
                map.put(entity.getId(), entity);
            }
            this.agentMap = map;
        } finally {
            lock.unlock();
        }
    }
}
