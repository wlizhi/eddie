package cc.wlizhi.eddieai.memory.context;

import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.memory.dao.ModelProviderDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 模型服务商上下文，全应用生命周期缓存
 * <p>
 * 以 {@code providerId} 为键快速查找，不再依赖 {@code providerCode} 模糊匹配。
 * ChatPolicy 策略匹配仍通过 {@code code}（业务类型）进行。
 */
@Component
public class ModelProviderContext {

    private volatile Map<Long, ModelProviderEntity> modelProviderMap;

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

    /**
     * 根据业务类型 code 获取服务商配置（仅限 ChatPolicy 策略匹配使用）
     * 注意：同 code 可能有多个实例，只返回排序第一个
     */
    public ModelProviderEntity getModelProvider(String providerCode) {
        for (ModelProviderEntity provider : modelProviderMap.values()) {
            if (Objects.equals(provider.getCode(), providerCode)) {
                return provider;
            }
        }
        refresh();
        for (ModelProviderEntity provider : modelProviderMap.values()) {
            if (Objects.equals(provider.getCode(), providerCode)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 获取全部缓存的服务商列表
     */
    public List<ModelProviderEntity> getAll() {
        return List.copyOf(modelProviderMap.values());
    }

    public void refresh() {
        List<ModelProviderEntity> all = modelProviderDao.findAll();
        Map<Long, ModelProviderEntity> map = new HashMap<>();
        for (ModelProviderEntity entity : all) {
            map.put(entity.getId(), entity);
        }
        this.modelProviderMap = map;
    }
}
