package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.dao.ModelProviderDao;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 模型服务商上下文，全应用生命周期缓存
 * <p>
 * 以 {@code providerId} 为键快速查找，不再依赖 {@code providerCode} 模糊匹配。
 * ChatPolicy 策略匹配仍通过 {@code code}（业务类型）进行。
 */
@Component
public class ModelProviderContext implements GlobalCache {

    private volatile Map<Long, ModelProviderEntity> modelProviderMap;
    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private ModelProviderDao modelProviderDao;

    @PostConstruct
    void init() {
        refresh();
    }

    /**
     * 根据 providerId 精确获取服务商配置
     */
    public ModelProviderEntity getModelProviderById(Long providerId) {
        ModelProviderEntity entity = modelProviderMap.get(providerId);
        if (entity != null) {
            return entity;
        }
        refresh();
        return modelProviderMap.get(providerId);
    }

    public void refresh() {
        try {
            lock.lock();
            List<ModelProviderEntity> all = modelProviderDao.findAll();
            Map<Long, ModelProviderEntity> map = new LinkedHashMap<>();
            for (ModelProviderEntity entity : all) {
                map.put(entity.getId(), entity);
            }
            this.modelProviderMap = map;
        } finally {
            lock.unlock();
        }
    }
}
