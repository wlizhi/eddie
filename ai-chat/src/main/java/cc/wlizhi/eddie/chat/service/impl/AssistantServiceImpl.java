/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import cc.wlizhi.eddie.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddie.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddie.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddie.chat.entity.response.AssistantVO;
import cc.wlizhi.eddie.chat.entity.response.ToolItemVO;
import cc.wlizhi.eddie.chat.entity.response.ToolSourceVO;
import cc.wlizhi.eddie.chat.service.AssistantService;
import cc.wlizhi.eddie.common.dao.*;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.common.util.FileStorageUtil;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 助手列表业务实现
 */
@Service
public class AssistantServiceImpl implements AssistantService {

    @Resource
    private AssistantContext assistantContext;

    @Resource
    private AssistantDao assistantDao;

    @Resource
    private SessionDao sessionDao;

    @Resource
    private MessageDao messageDao;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Resource
    private ToolDefinitionDao toolDefinitionDao;

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<AssistantVO> list(boolean showAll) {
        List<AssistantEntity> entities = assistantDao.findAll(showAll);
        List<AssistantVO> result = new ArrayList<>();
        for (AssistantEntity entity : entities) {
            ModelProviderEntity modelProvider = modelProviderContext.getModelProviderById(entity.getProviderId());
            AssistantVO assistant = toVO(entity);
            if (modelProvider != null) {
                assistant.setProviderName(modelProvider.getName());
            }
            result.add(assistant);
        }
        return result;
    }

    @Override
    public AssistantDetailVO getDetail(Long id) {
        AssistantEntity entity = assistantDao.findById(id);
        if (entity == null) {
            throw new NotFoundException("助手不存在: " + id);
        }
        ModelProviderEntity modelProvider = modelProviderContext.getModelProviderById(entity.getProviderId());
        AssistantDetailVO assistant = toDetailVO(entity);
        if (modelProvider != null) {
            assistant.setProviderCode(modelProvider.getCode());
            assistant.setProviderName(modelProvider.getName());
        }
        return assistant;
    }

    @Override
    @Transactional
    public AssistantVO create(AssistantCreateRequest request) {
        AssistantEntity entity = new AssistantEntity();
        entity.setName(request.getName());
        entity.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");
        entity.setProviderId(request.getProviderId());
        entity.setModelId(request.getModelId() == null ? "" : request.getModelId());
        entity.setModelParams(serializeModelParams(request.getModelParams()));
        entity.setPreferences(serializePreferences(request.getPreferences()));
        entity.setMemoryRounds(request.getMemoryRounds() != null ? request.getMemoryRounds() : 20);
        entity.setEnabled(1);
        entity.setSortOrder(0);

        assistantDao.insert(entity);
        Long assistantId = assistantDao.findLastInsertId();

        // 处理工具绑定
        bindMcpServerTools(assistantId, request.getEnabledMcpServerIds());

        // 刷新全局缓存
        assistantContext.refresh();

        // 插入后查询完整数据（含自增 ID）
        AssistantEntity saved = assistantDao.findById(assistantId);
        return toVO(saved);
    }

    @Override
    @Transactional
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
        entity.setPreferences(serializePreferences(request.getPreferences()));
        entity.setMemoryRounds(request.getMemoryRounds());
        entity.setEnabled(request.getEnabled());
        entity.setSortOrder(request.getSortOrder());

        assistantDao.update(entity);

        // 处理工具绑定（全量替换）
        bindMcpServerTools(id, request.getEnabledMcpServerIds());

        assistantContext.refresh();

        AssistantEntity updated = assistantDao.findById(id);
        return toVO(updated);
    }

    @Override
    public AssistantVO updateAvatar(Long id, String avatarText, MultipartFile file) {
        // 1. 校验助手存在
        AssistantEntity old = assistantContext.getAssistantById(id);
        if (old == null) {
            throw new NotFoundException("助手不存在: " + id);
        }

        // 2. 计算新头像值
        String newAvatar = "";
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
        }

        // 3. 旧头像如果是图片路径 → 删除旧文件
        String oldAvatar = old.getAvatar();
        if (FileStorageUtil.isFileUrl(oldAvatar)) {
            FileStorageUtil.delete(oldAvatar);
        }

        // 4. 更新 DB
        assistantDao.updateAvatar(id, newAvatar);
        // 5. 刷新缓存
        assistantContext.refresh();

        // 6. 返回更新后的助手
        AssistantEntity updated = assistantDao.findById(id);
        return toVO(updated);
    }

    @Override
    public void delete(Long id) {
        if (!assistantDao.existsById(id)) {
            throw new NotFoundException("助手不存在: " + id);
        }
        // 级联删除：消息 → 会话 → 助手
        messageDao.deleteByAssistantId(id);
        sessionDao.deleteByAssistantId(id);
        assistantDao.deleteById(id);
        ownerToolBindingDao.deleteByOwner(RoleType.ASSISTANT, id);
        // 刷新缓存
        assistantContext.refresh();
        ownerToolBindingContext.refresh();
    }

    @Override
    public void batchSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            assistantDao.updateSortOrder(ids.get(i), i + 1);
        }
        assistantContext.refresh();
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
        vo.setModelId(entity.getModelId());
        vo.setMemoryRounds(entity.getMemoryRounds());
        vo.setEnabled(entity.getEnabled());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @Override
    public List<ToolSourceVO> getBoundMcpTools(Long assistantId) {
        // 1. 获取助手绑定的工具列表（已过滤全局启用 + MCP 服务启用）
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(RoleType.ASSISTANT.name(), assistantId);
        if (boundTools.isEmpty()) {
            return List.of();
        }

        // 2. 提取 MCP Server ID，构建工具映射：mcpServerId → 工具列表
        Map<Long, List<ToolDefinitionEntity>> mcpToolsMap = new LinkedHashMap<>();
        for (ToolDefinitionEntity tool : boundTools) {
            Long mcpServerId = tool.getMcpServerId();
            if (mcpServerId == null) continue; // 跳过无关联 MCP 的内置工具
            mcpToolsMap.computeIfAbsent(mcpServerId, k -> new ArrayList<>()).add(tool);
        }

        if (mcpToolsMap.isEmpty()) {
            return List.of();
        }

        // 3. 获取 MCP Server 实体并排序（按 sortOrder）
        List<McpServerEntity> mcpServers = mcpToolsMap.keySet().stream()
                .map(ownerToolBindingContext::getMcpServer)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0))
                .toList();

        // 4. 组装二层 VO
        List<ToolSourceVO> result = new ArrayList<>(mcpServers.size());
        for (McpServerEntity server : mcpServers) {
            List<ToolDefinitionEntity> tools = mcpToolsMap.get(server.getId());
            if (tools == null || tools.isEmpty()) continue;

            List<ToolItemVO> toolItems = tools.stream().map(t -> {
                ToolItemVO vo = new ToolItemVO();
                vo.setId(t.getId());
                vo.setName(t.getName());
                vo.setDisplayName(t.getDisplayName());
                vo.setDescription(t.getDescription());
                vo.setToolType(t.getToolType() != null ? t.getToolType().name() : "");
                vo.setEnabled(t.getEnabled() == 1);
                return vo;
            }).collect(Collectors.toList());

            ToolSourceVO source = new ToolSourceVO();
            source.setMcpServerId(server.getId());
            source.setMcpServerName(server.getName());
            source.setTransportType(server.getTransportType());
            source.setEnabled(server.getEnabled() == 1);
            source.setTools(toolItems);
            source.setBound(true); // 此接口返回的必定已绑定
            result.add(source);
        }
        return result;
    }

    /**
     * 绑定指定 MCP Server 下的所有工具到 Owner
     * <p>
     * 全量替换：先清旧绑定，再插新绑定。完成后刷新 OwnerToolBindingContext 缓存。
     */
    private void bindMcpServerTools(Long ownerId, List<Long> mcpServerIds) {
        // 先清除旧的绑定
        ownerToolBindingDao.deleteByOwner(RoleType.ASSISTANT, ownerId);

        if (mcpServerIds != null && !mcpServerIds.isEmpty()) {
            List<Long> toolIds = new ArrayList<>();
            for (Long mcpServerId : mcpServerIds) {
                List<ToolDefinitionEntity> tools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
                if (tools != null) {
                    toolIds.addAll(tools.stream().map(ToolDefinitionEntity::getId).toList());
                }
            }
            if (!toolIds.isEmpty()) {
                // 批量插入
                ownerToolBindingDao.batchInsert(RoleType.ASSISTANT, ownerId, toolIds);
            }
        }
        // 刷新工具绑定缓存
        ownerToolBindingContext.refresh();
    }

    private AssistantDetailVO toDetailVO(AssistantEntity entity) {
        AssistantDetailVO vo = new AssistantDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setProviderId(entity.getProviderId());
        vo.setModelId(entity.getModelId());
        vo.setModelParams(deserializeModelParams(entity.getModelParams()));
        vo.setPreferences(deserializePreferences(entity.getPreferences()));
        vo.setMemoryRounds(entity.getMemoryRounds());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        // 回显已绑定的 MCP Server ID（走缓存）
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools("ASSISTANT", entity.getId());
        List<Long> boundIds = boundTools.stream()
                .filter(t -> t.getMcpServerId() != null)
                .map(ToolDefinitionEntity::getMcpServerId)
                .distinct()
                .collect(Collectors.toList());
        vo.setBoundMcpServerIds(boundIds);
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

    private String serializePreferences(java.util.Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(preferences);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private java.util.Map<String, Object> deserializePreferences(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new java.util.HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<java.util.Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return new java.util.HashMap<>();
        }
    }
}
