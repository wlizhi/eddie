package cc.wlizhi.eddieai.memory.context;

import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.memory.dao.ModelProviderDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 模型服务商、模型列表上下文，全应用生命周期
 */
@Component
public class ModelProviderContext {
    private volatile List<ModelProviderEntity> modelProviderEntities;
    private volatile Map<String, String> modelProviderMap;

    @Resource
    private ModelProviderDao modelProviderDao;

    @PostConstruct
    void init() {
        refresh();
    }

    public String getProvider(String modelId) {
        return modelProviderMap.get(modelId);
    }

    public ModelProviderEntity getModelProvider(String providerCode) {
        for (ModelProviderEntity provider : modelProviderEntities) {
            if (Objects.equals(provider.getCode(), providerCode)) {
                return provider;
            }
        }
        refresh();
        for (ModelProviderEntity provider : modelProviderEntities) {
            if (Objects.equals(provider.getCode(), providerCode)) {
                return provider;
            }
        }
        return null;
    }


    public void refresh() {
        this.modelProviderEntities = modelProviderDao.findAll();
        Map<String, String> map = new HashMap<>();
        for (ModelProviderEntity entity : modelProviderEntities) {
            String models = entity.getModels();
            if (ObjectUtils.isEmpty(models)) {
                continue;
            }
            // TODO 待办：多模型服务商策略兼容
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> modelInfoMaps = objectMapper.readValue(models, new TypeReference<List<Map<String, Object>>>() {
            });
            for (Map<String, Object> modelInfoMap : modelInfoMaps) {
                String id = modelInfoMap.get("id").toString();
                String ownedBy = modelInfoMap.get("owned_by").toString();
                map.put(id, ownedBy);
            }
        }
        modelProviderMap = map;
    }
}
