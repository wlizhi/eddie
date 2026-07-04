/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.dao.ModelProviderDao;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import cc.wlizhi.eddie.settings.entity.request.ModelBatchAddRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelBatchRemoveRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelUpdateRequest;
import cc.wlizhi.eddie.settings.service.ModelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelServiceImpl implements ModelService {

    @Resource
    private ModelProviderDao modelProviderDao;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void updateModel(Long providerId, ModelUpdateRequest request) {
        if (providerId == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }

        ModelProviderEntity entity = modelProviderDao.findById(providerId);
        if (entity == null) {
            throw new NotFoundException("服务商不存在: " + providerId);
        }

        // 解析现有 models
        List<Map<String, Object>> models = parseModels(entity.getModels());

        // 按 code 查找并更新
        String code = request.getCode().trim();
        boolean found = false;
        for (Map<String, Object> model : models) {
            if (code.equals(model.get("id"))) {
                // 只更新非 null 字段
                if (request.getName() != null) {
                    model.put("name", request.getName());
                }
                model.put("capabilities", request.getCapabilities() == null ? List.of() : request.getCapabilities());
                model.put("currency", request.getCurrency() == null ? "" : request.getCurrency());
                model.put("input_price", request.getInputPrice());
                model.put("output_price", request.getOutputPrice());
                model.put("cache_input_price", request.getCacheInputPrice());
                model.put("cache_write_input_price", request.getCacheWriteInputPrice());
                model.put("call_interval_sec", request.getCallIntervalSec());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new NotFoundException("服务商下未找到模型: " + code);
        }

        // 序列化并保存
        entity.setModels(serializeModels(models));
        modelProviderDao.update(entity);
        modelProviderContext.refresh();
    }

    @Override
    public void batchAddModels(Long providerId, ModelBatchAddRequest request) {
        if (providerId == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }
        if (request.getModels() == null || request.getModels().isEmpty()) {
            return;
        }

        ModelProviderEntity entity = modelProviderDao.findById(providerId);
        if (entity == null) {
            throw new NotFoundException("服务商不存在: " + providerId);
        }

        List<Map<String, Object>> models = parseModels(entity.getModels());

        for (ModelBatchAddRequest.BatchAddItem item : request.getModels()) {
            String code = item.getCode();
            if (code == null || code.isBlank()) continue;

            // 跳过已存在的
            boolean exists = models.stream().anyMatch(m -> code.equals(m.get("id")));
            if (exists) continue;

            Map<String, Object> newModel = new LinkedHashMap<>();
            newModel.put("id", code.trim());
            if (item.getName() != null) newModel.put("name", item.getName());
            if (item.getObject() != null) newModel.put("object", item.getObject());
            if (item.getOwnedBy() != null) newModel.put("owned_by", item.getOwnedBy());
            if (item.getCapabilities() != null) newModel.put("capabilities", item.getCapabilities());
            if (item.getCurrency() != null) newModel.put("currency", item.getCurrency());
            if (item.getInputPrice() != null) newModel.put("input_price", item.getInputPrice());
            if (item.getOutputPrice() != null) newModel.put("output_price", item.getOutputPrice());
            if (item.getCacheInputPrice() != null) newModel.put("cache_input_price", item.getCacheInputPrice());
            if (item.getCacheWriteInputPrice() != null)
                newModel.put("cache_write_input_price", item.getCacheWriteInputPrice());

            models.add(newModel);
        }

        entity.setModels(serializeModels(models));
        modelProviderDao.update(entity);
        modelProviderContext.refresh();
    }

    @Override
    public void batchRemoveModels(Long providerId, ModelBatchRemoveRequest request) {
        if (providerId == null) {
            throw new BadRequestException("服务商 ID 不能为空");
        }
        if (request.getCodes() == null || request.getCodes().isEmpty()) {
            return;
        }

        ModelProviderEntity entity = modelProviderDao.findById(providerId);
        if (entity == null) {
            throw new NotFoundException("服务商不存在: " + providerId);
        }

        List<Map<String, Object>> models = parseModels(entity.getModels());

        models.removeIf(m -> {
            Object id = m.get("id");
            return id != null && request.getCodes().contains(id.toString());
        });

        entity.setModels(serializeModels(models));
        modelProviderDao.update(entity);
        modelProviderContext.refresh();
    }

    // ========== 私有方法 ==========

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseModels(String modelsJson) {
        if (modelsJson == null || modelsJson.isEmpty() || "[]".equals(modelsJson)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(modelsJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("解析模型列表 JSON 失败: " + e.getMessage(), e);
        }
    }

    private String serializeModels(List<Map<String, Object>> models) {
        try {
            return objectMapper.writeValueAsString(models);
        } catch (Exception e) {
            throw new RuntimeException("序列化模型列表 JSON 失败: " + e.getMessage(), e);
        }
    }
}
