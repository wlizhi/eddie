/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.dao.ModelProviderDao;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.enums.ModelCapability;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.ConflictException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import cc.wlizhi.eddie.settings.entity.request.ModelProviderCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelProviderUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;
import cc.wlizhi.eddie.settings.remote.ModelCapabilityResolver;
import cc.wlizhi.eddie.settings.remote.RemoteModelFetcherRouter;
import cc.wlizhi.eddie.settings.service.ModelProviderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ModelProviderServiceImpl implements ModelProviderService {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ModelProviderDao modelProviderDao;

    @Resource
    private RemoteModelFetcherRouter remoteModelFetcherRouter;

    @Resource
    private ModelCapabilityResolver modelCapabilityResolver;

    @Resource
    private ObjectMapper objectMapper;

    private volatile WeakReference<Map<Long, ModelCache>> remoteModelsCache = new WeakReference<>(new ConcurrentHashMap<>());

    @Override
    public List<ModelProviderVO> listAll() {
        List<ModelProviderEntity> entities = modelProviderDao.findAll();

        // 排序：1级 enabled 启用在前禁用在后，2级 sort_order 升序，3级 id 正序
        entities.sort(Comparator
                .comparing(ModelProviderEntity::getEnabled, Comparator.reverseOrder())
                .thenComparing(ModelProviderEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ModelProviderEntity::getId));

        return transformToProviderVOS(entities);
    }

    @Override
    public List<ModelProviderVO> listWithModels() {
        List<ModelProviderEntity> entities = modelProviderDao.findAll();

        entities.sort(Comparator
                .comparing(ModelProviderEntity::getEnabled, Comparator.reverseOrder())
                .thenComparing(ModelProviderEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ModelProviderEntity::getId));

        List<ModelProviderVO> result = new ArrayList<>();
        for (ModelProviderEntity entity : entities) {
            ModelProviderVO vo = transformToVO(entity);
            vo.setModels(parseModelsJson(entity.getModels()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<ModelVO> getModelsByCode(String code) {
        String modelsJson = modelProviderDao.findModelsByCode(code);
        if (modelsJson == null || modelsJson.isEmpty() || "[]".equals(modelsJson)) {
            return new ArrayList<>();
        }
        return parseModelsJson(modelsJson);
    }

    @Override
    public void create(ModelProviderCreateRequest request) {
        // 校验必传参数
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("服务商 code 不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("服务商名称不能为空");
        }
        if (request.getBaseUrl() == null || request.getBaseUrl().isBlank()) {
            throw new BadRequestException("API 地址不能为空");
        }

        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName().trim());
        entity.setBaseUrl(request.getBaseUrl().trim());
        entity.setApiKey(request.getApiKey() != null ? request.getApiKey() : "");
        entity.setModels(request.getModels() != null ? request.getModels() : "[]");
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        entity.setBuiltIn(request.getBuiltIn() != null ? request.getBuiltIn() : 0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        try {
            modelProviderDao.insert(entity);
        } catch (UncategorizedSQLException ex) {
            throw new ConflictException("服务商 code 已存在: " + entity.getCode());
        }
        modelProviderContext.refresh();
    }

    @Override
    public void update(ModelProviderUpdateRequest request) {
        if (request.getId() == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }

        ModelProviderEntity existing = modelProviderDao.findById(request.getId());
        if (existing == null) {
            throw new NotFoundException("服务商不存在: " + request.getId());
        }

        // 内置记录：code 和 name 不可修改
        if (existing.getBuiltIn() == 1) {
            if (request.getName() != null && !request.getName().equals(existing.getName())) {
                throw new BadRequestException("内置服务商名称不可修改");
            }
        }

        // 只更新非 null 字段
        if (request.getName() != null) {
            existing.setName(request.getName().trim());
        }
        if (request.getBaseUrl() != null) {
            existing.setBaseUrl(request.getBaseUrl().trim());
        }
        if (request.getApiKey() != null) {
            existing.setApiKey(request.getApiKey());
        }
        if (request.getModels() != null) {
            existing.setModels(request.getModels());
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }

        modelProviderDao.update(existing);
        modelProviderContext.refresh();
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }

        ModelProviderEntity existing = modelProviderDao.findById(id);
        if (existing == null) {
            throw new NotFoundException("服务商不存在: " + id);
        }
        if (existing.getBuiltIn() == 1) {
            throw new BadRequestException("内置服务商不可删除");
        }

        modelProviderDao.deleteById(id);
        modelProviderContext.refresh();
    }

    @Override
    public void updateSortOrder(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            modelProviderDao.updateSortOrder(orderedIds.get(i), i + 1);
        }
        modelProviderContext.refresh();
    }

    @Override
    public List<ModelVO> fetchRemoteModels(Long providerId) {
        if (providerId == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }
        ModelProviderEntity entity = modelProviderContext.getModelProviderById(providerId);
        if (entity == null) {
            throw new NotFoundException("服务商不存在: " + providerId);
        }

        Map<Long, ModelCache> cacheMap = remoteModelsCache.get();
        if (cacheMap == null) {
            cacheMap = new ConcurrentHashMap<>();
        }
        ModelCache modelCache = cacheMap.get(providerId);
        if (modelCache != null && modelCache.expireTime + Duration.ofMinutes(30).toMillis() > System.currentTimeMillis()) {
            return modelCache.models;
        }

        List<ModelVO> models = remoteModelFetcherRouter.fetchModels(entity.getCode(), entity.getBaseUrl(), entity.getApiKey());
        // 填充能力标签：查映射表 → 兜底关键词推断
        String providerCode = entity.getCode();
        for (ModelVO model : models) {
            model.setCapabilities(modelCapabilityResolver.resolve(providerCode, model.getCode()));
        }

        cacheMap.put(providerId, new ModelCache(System.currentTimeMillis(), models));
        remoteModelsCache = new WeakReference<>(cacheMap);
        return models;
    }

    // ========== 私有方法 ==========

    private @NonNull List<ModelProviderVO> transformToProviderVOS(List<ModelProviderEntity> entities) {
        List<ModelProviderVO> result = new ArrayList<>();
        for (ModelProviderEntity entity : entities) {
            result.add(transformToVO(entity));
        }
        return result;
    }

    private @NonNull ModelProviderVO transformToVO(ModelProviderEntity entity) {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setApiKey(entity.getApiKey());
        vo.setEnabled(entity.getEnabled());
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private @NonNull List<ModelVO> parseModelsJson(String modelsJson) {
        if (modelsJson == null || modelsJson.isEmpty() || "[]".equals(modelsJson)) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(modelsJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<ModelVO> result = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                ModelVO vo = new ModelVO();
                Object idObj = raw.get("id");
                String modelId = idObj != null ? idObj.toString() : null;
                vo.setCode(modelId);
                Object objectObj = raw.get("object");
                vo.setObject(objectObj != null ? objectObj.toString() : null);
                Object ownedByObj = raw.get("owned_by");
                vo.setOwnedBy(ownedByObj != null ? ownedByObj.toString() : null);
                // capabilities：优先读 JSON 中已有标签，没有则从模型 ID 关键词推断
                vo.setCapabilities(parseCapabilities(raw, modelId));
                // 定价信息
                Object currencyObj = raw.get("currency");
                vo.setCurrency(currencyObj != null ? currencyObj.toString() : null);
                vo.setInputPrice(parseDouble(raw.get("input_price")));
                vo.setOutputPrice(parseDouble(raw.get("output_price")));
                vo.setCacheInputPrice(parseDouble(raw.get("cache_input_price")));
                vo.setCacheWriteInputPrice(parseDouble(raw.get("cache_write_input_price")));
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("解析模型列表 JSON 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析模型能力标签，优先读 JSON 中的 capabilities，否则根据模型 ID 关键词推断
     */
    @SuppressWarnings("unchecked")
    private List<ModelCapability> parseCapabilities(Map<String, Object> raw, String modelId) {
        // 1) 如果 JSON 中有 capabilities 字段，直接解析
        Object capsObj = raw.get("capabilities");
        if (capsObj instanceof List) {
            List<String> capCodes = (List<String>) capsObj;
            List<ModelCapability> caps = capCodes.stream()
                    .map(ModelCapability::fromCode)
                    .filter(c -> c != null)
                    .collect(Collectors.toList());
            if (!caps.isEmpty()) {
                return caps;
            }
        }
        // 2) 兜底：根据模型 ID 关键词推断
        return inferCapabilities(modelId);
    }

    /**
     * 根据模型 ID 关键词推断能力标签
     */
    private List<ModelCapability> inferCapabilities(String modelId) {
        if (modelId == null) {
            return List.of(ModelCapability.FUNCTION_CALLING);
        }
        String id = modelId.toLowerCase();
        // 嵌入模型
        if (id.contains("embedding") || id.contains("embed")) {
            return List.of(ModelCapability.EMBEDDING);
        }
        // 重排模型
        if (id.contains("rerank")) {
            return List.of(ModelCapability.RERANK);
        }
        List<ModelCapability> caps = new ArrayList<>();
        // 视觉
        if (id.contains("vision") || id.contains("vl") || id.contains("multimodal")) {
            caps.add(ModelCapability.VISION);
        }
        // 联网搜索
        if (id.contains("search") || id.contains("web")) {
            caps.add(ModelCapability.WEB_SEARCH);
        }
        // 推理（o1/o3/r1/deepseek-r1/reasoning 等）
        if (id.startsWith("o1-") || id.startsWith("o1/")
                || id.startsWith("o3-") || id.startsWith("o3/")
                || id.contains("r1") || id.contains("reasoning")) {
            caps.add(ModelCapability.REASONING);
        }
        // 工具调用：所有非嵌入/重排的聊天模型都支持
        caps.add(ModelCapability.FUNCTION_CALLING);
        return caps;
    }

    /**
     * 安全地将 Object 转为 Double，非数字或 null 时返回 null
     */
    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record ModelCache(long expireTime, List<ModelVO> models) {
    }
}
