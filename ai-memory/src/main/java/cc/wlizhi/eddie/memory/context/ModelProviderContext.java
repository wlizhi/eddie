/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.ModelProviderDao;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import cc.wlizhi.eddie.memory.cache.ModelThrottleCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
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
    @Resource
    private InitScheduler initScheduler;
    @Resource
    private ObjectProvider<ModelThrottleCache> modelThrottleCacheProvider;
    @Resource
    private ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1000, this::refresh);
    }

    /**
     * 获取所有模型服务商列表
     */
    public List<ModelProviderEntity> listAll() {
        Map<Long, ModelProviderEntity> map = modelProviderMap;
        if (map == null) {
            refresh();
            map = modelProviderMap;
        }
        return new ArrayList<>(map.values());
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

    public List<ModelJsonItem> getModelsByProviderId(Long providerId) {
        ModelProviderEntity entity = modelProviderMap.get(providerId);
        if (ObjectUtils.isEmpty(entity.getModels())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(entity.getModels(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
            // 联动刷新：模型配置变更时同步更新节流阀缓存
            modelThrottleCacheProvider.getIfAvailable().refresh();
        } finally {
            lock.unlock();
        }
    }
}
