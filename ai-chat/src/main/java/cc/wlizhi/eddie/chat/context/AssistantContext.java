package cc.wlizhi.eddie.chat.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.AssistantDao;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 助手列表上下文，全应用生命周期缓存
 * <p>
 * 以 {@code assistantId} 为键快速查找助手信息，包括系统提示词、模型参数等。
 */
@Component
public class AssistantContext implements GlobalCache {

    private volatile Map<Long, AssistantEntity> assistantMap;
    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private AssistantDao assistantDao;
    @Resource
    private InitScheduler initScheduler;

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1000, this::refresh);
    }

    /**
     * 根据 assistantId 精确获取助手信息
     */
    public AssistantEntity getAssistantById(Long assistantId) {
        AssistantEntity entity = assistantMap.get(assistantId);
        if (entity != null) {
            return entity;
        }
        refresh();
        return assistantMap.get(assistantId);
    }

    public void refresh() {
        try {
            lock.lock();
            List<AssistantEntity> all = assistantDao.findAll(true);
            Map<Long, AssistantEntity> map = new LinkedHashMap<>();
            for (AssistantEntity entity : all) {
                map.put(entity.getId(), entity);
            }
            this.assistantMap = map;
        } finally {
            lock.unlock();
        }
    }
}
