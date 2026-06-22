package cc.wlizhi.eddieai.settings.service.impl;

import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.common.exception.ConflictException;
import cc.wlizhi.eddieai.common.exception.NotFoundException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import cc.wlizhi.eddieai.settings.dao.ModelProviderMapper;
import cc.wlizhi.eddieai.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;
import cc.wlizhi.eddieai.settings.service.ModelProviderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ModelProviderServiceImpl implements ModelProviderService {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ModelProviderMapper modelProviderMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ModelProviderVO> listAll() {
        List<ModelProviderEntity> entities = modelProviderMapper.findAll();

        // 排序：sort_order 升序（null 排最后），再按 code ASCII 升序
        entities.sort(Comparator
                .comparing(ModelProviderEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ModelProviderEntity::getCode));

        return transformToProviderVOS(entities);
    }

    private static @NonNull List<ModelProviderVO> transformToProviderVOS(List<ModelProviderEntity> entities) {
        List<ModelProviderVO> result = new ArrayList<>();
        for (ModelProviderEntity entity : entities) {
            ModelProviderVO vo = new ModelProviderVO();
            vo.setCode(entity.getCode());
            vo.setName(entity.getName());
            vo.setBaseUrl(entity.getBaseUrl());
            vo.setApiKey(entity.getApiKey());
            vo.setEnabled(entity.getEnabled());
            vo.setSortOrder(entity.getSortOrder());
            vo.setCreatedAt(entity.getCreatedAt());
            vo.setUpdatedAt(entity.getUpdatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<ModelVO> getModelsByCode(String code) {
        String modelsJson = modelProviderMapper.findModelsByCode(code);
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
                // id → code, object → object, owned_by → ownedBy
                Object idObj = raw.get("id");
                vo.setCode(idObj != null ? idObj.toString() : null);
                Object objectObj = raw.get("object");
                vo.setObject(objectObj != null ? objectObj.toString() : null);
                Object ownedByObj = raw.get("owned_by");
                vo.setOwnedBy(ownedByObj != null ? ownedByObj.toString() : null);
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("解析模型列表 JSON 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void create(ModelProviderEntity entity) {
        if (entity.getCode() == null || entity.getCode().isEmpty()) {
            throw new BadRequestException("服务商 code 不能为空");
        }
        try {
            modelProviderMapper.insert(entity);
        } catch (UncategorizedSQLException ex) {
            throw new ConflictException("服务商 code 已存在: " + entity.getCode());
        }
        modelProviderContext.refresh();
    }

    @Override
    public void update(ModelProviderEntity entity) {
        if (entity.getCode() == null || entity.getCode().isEmpty()) {
            throw new BadRequestException("服务商 code 不能为空");
        }
        if (!modelProviderMapper.existsByCode(entity.getCode())) {
            throw new NotFoundException("服务商不存在: " + entity.getCode());
        }
        modelProviderMapper.update(entity);
        modelProviderContext.refresh();
    }

    @Override
    public void deleteByCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new BadRequestException("服务商 code 不能为空");
        }
        if (!modelProviderMapper.existsByCode(code)) {
            throw new NotFoundException("服务商不存在: " + code);
        }
        modelProviderMapper.deleteByCode(code);
        modelProviderContext.refresh();
    }
}
