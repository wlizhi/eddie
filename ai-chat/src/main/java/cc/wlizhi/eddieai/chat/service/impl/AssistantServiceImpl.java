package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.dao.AssistantDao;
import cc.wlizhi.eddieai.chat.entity.AssistantEntity;
import cc.wlizhi.eddieai.chat.entity.dto.ModelParams;
import cc.wlizhi.eddieai.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddieai.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddieai.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddieai.chat.entity.response.AssistantVO;
import cc.wlizhi.eddieai.chat.service.AssistantService;
import cc.wlizhi.eddieai.common.exception.NotFoundException;
import cc.wlizhi.eddieai.common.util.FileStorageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 助手列表业务实现
 */
@Service
public class AssistantServiceImpl implements AssistantService {

    @Resource
    private AssistantDao assistantDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

    @Override
    public List<AssistantVO> list(boolean showAll) {
        List<AssistantEntity> entities = assistantDao.findAll(showAll);
        List<AssistantVO> result = new ArrayList<>();
        for (AssistantEntity entity : entities) {
            result.add(toVO(entity));
        }
        return result;
    }

    @Override
    public AssistantDetailVO getDetail(Long id) {
        AssistantEntity entity = assistantDao.findById(id);
        if (entity == null) {
            throw new NotFoundException("助手不存在: " + id);
        }
        return toDetailVO(entity);
    }

    @Override
    public AssistantVO create(AssistantCreateRequest request) {
        AssistantEntity entity = new AssistantEntity();
        entity.setName(request.getName());
        entity.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");
        entity.setProviderId(request.getProviderId());
        entity.setModelId(request.getModelId());
        entity.setModelParams(serializeModelParams(request.getModelParams()));
        entity.setMemoryRounds(request.getMemoryRounds() != null ? request.getMemoryRounds() : 20);
        entity.setEnabled(1);
        entity.setSortOrder(0);

        assistantDao.insert(entity);

        // 插入后查询完整数据（含自增 ID）
        AssistantEntity saved = assistantDao.findById(
                assistantDao.findLastInsertId());
        return toVO(saved);
    }

    @Override
    public AssistantVO update(Long id, AssistantUpdateRequest request) {
        if (!assistantDao.existsById(id)) {
            throw new NotFoundException("助手不存在: " + id);
        }

        AssistantEntity entity = new AssistantEntity();
        entity.setId(id);
        entity.setName(request.getName());
        entity.setAvatar(request.getAvatar());
        entity.setDescription(request.getDescription());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setProviderId(request.getProviderId());
        entity.setModelId(request.getModelId());
        entity.setModelParams(serializeModelParams(request.getModelParams()));
        entity.setMemoryRounds(request.getMemoryRounds());
        entity.setEnabled(request.getEnabled());
        entity.setSortOrder(request.getSortOrder());

        assistantDao.update(entity);

        AssistantEntity updated = assistantDao.findById(id);
        return toVO(updated);
    }

    @Override
    public AssistantVO updateAvatar(Long id, String avatarText, MultipartFile file) {
        // 1. 校验助手存在
        AssistantEntity old = assistantDao.findById(id);
        if (old == null) {
            throw new NotFoundException("助手不存在: " + id);
        }

        // 2. 计算新头像值
        String newAvatar;
        if (file != null && !file.isEmpty()) {
            // 上传图片 → 保存到磁盘
            try {
                byte[] data = file.getBytes();
                // 提取扩展名，默认 webp
                String ext = "webp";
                String originalName = file.getOriginalFilename();
                if (originalName != null && originalName.contains(".")) {
                    String origExt = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
                    if (origExt.equals("png") || origExt.equals("jpg") || origExt.equals("jpeg")
                            || origExt.equals("gif") || origExt.equals("webp")) {
                        ext = origExt;
                    }
                }
                newAvatar = FileStorageUtil.save(data, ext);
            } catch (IOException e) {
                throw new RuntimeException("读取上传文件失败", e);
            }
        } else if (avatarText != null && !avatarText.isEmpty()) {
            // 文字/emoji → 直接存
            newAvatar = avatarText;
        } else {
            throw new IllegalArgumentException("请提供头像文字或图片");
        }

        // 3. 旧头像如果是图片路径 → 删除旧文件
        String oldAvatar = old.getAvatar();
        if (FileStorageUtil.isFileUrl(oldAvatar)) {
            FileStorageUtil.delete(oldAvatar);
        }

        // 4. 更新 DB
        assistantDao.updateAvatar(id, newAvatar);

        // 5. 返回更新后的助手
        AssistantEntity updated = assistantDao.findById(id);
        return toVO(updated);
    }

    @Override
    public void delete(Long id) {
        if (!assistantDao.existsById(id)) {
            throw new NotFoundException("助手不存在: " + id);
        }
        // TODO: 级联删除该助手的全部会话（会话表待实现）
        // chatSessionMapper.deleteByAssistantId(id);
        assistantDao.deleteById(id);
    }

    @Override
    public void batchSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            assistantDao.updateSortOrder(ids.get(i), i + 1);
        }
    }

    // ==================== 内部方法 ====================

    private AssistantVO toVO(AssistantEntity entity) {
        AssistantVO vo = new AssistantVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setProviderId(entity.getProviderId());
        vo.setProviderName(entity.getProviderName());
        vo.setModelId(entity.getModelId());
        vo.setMemoryRounds(entity.getMemoryRounds());
        vo.setEnabled(entity.getEnabled());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AssistantDetailVO toDetailVO(AssistantEntity entity) {
        AssistantDetailVO vo = new AssistantDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setProviderId(entity.getProviderId());
        vo.setProviderCode(entity.getProviderCode());
        vo.setProviderName(entity.getProviderName());
        vo.setModelId(entity.getModelId());
        vo.setModelParams(deserializeModelParams(entity.getModelParams()));
        vo.setMemoryRounds(entity.getMemoryRounds());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String serializeModelParams(ModelParams params) {
        if (params == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 ModelParams 失败", e);
        }
    }

    private ModelParams deserializeModelParams(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new ModelParams();
        }
        try {
            return objectMapper.readValue(json, ModelParams.class);
        } catch (JsonProcessingException e) {
            return new ModelParams();
        }
    }
}
