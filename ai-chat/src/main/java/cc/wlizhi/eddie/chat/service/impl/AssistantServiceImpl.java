package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import cc.wlizhi.eddie.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddie.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddie.chat.entity.response.*;
import cc.wlizhi.eddie.chat.service.AssistantService;
import cc.wlizhi.eddie.common.dao.*;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.common.util.FileStorageUtil;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

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
        entity.setModelId(request.getModelId());
        entity.setModelParams(serializeModelParams(request.getModelParams()));
        entity.setMemoryRounds(request.getMemoryRounds() != null ? request.getMemoryRounds() : 20);
        entity.setEnabled(1);
        entity.setSortOrder(0);

        assistantDao.insert(entity);
        Long assistantId = assistantDao.findLastInsertId();

        // 处理工具绑定
        bindMcpServerTools(RoleType.ASSISTANT, assistantId, request.getEnabledMcpServerIds());

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
        entity.setMemoryRounds(request.getMemoryRounds());
        entity.setEnabled(request.getEnabled());
        entity.setSortOrder(request.getSortOrder());

        assistantDao.update(entity);

        // 处理工具绑定（全量替换）
        if (request.getEnabledMcpServerIds() != null) {
            bindMcpServerTools(RoleType.ASSISTANT, id, request.getEnabledMcpServerIds());
        }

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
        // 刷新缓存
        assistantContext.refresh();
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
    public List<ToolSourceVO> getToolSources(Long assistantId) {
        // 走缓存：获取全量 MCP + 工具二层结构
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();

        // 查询当前助手已绑定的 MCP Server ID 集合（走缓存）
        Set<Long> boundServerIds = Collections.emptySet();
        if (assistantId != null) {
            List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools("ASSISTANT", assistantId);
            boundServerIds = boundTools.stream()
                    .filter(t -> t.getMcpServerId() != null)
                    .map(ToolDefinitionEntity::getMcpServerId)
                    .collect(Collectors.toSet());
        }

        List<ToolSourceVO> result = new ArrayList<>();
        for (OwnerToolBindingContext.McpServerWithTools item : allData) {
            McpServerEntity server = item.mcpServer();
            List<ToolDefinitionEntity> tools = item.tools();

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
            source.setBound(boundServerIds.contains(server.getId()));
            result.add(source);
        }
        return result;
    }

    @Override
    public List<McpBindVO> getMcpBindings(Long assistantId) {
        // 走缓存：获取全量 MCP 列表
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();

        // 查询当前助手已绑定的 MCP Server ID 集合（走缓存）
        Set<Long> boundServerIds = Collections.emptySet();
        if (assistantId != null) {
            List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools("ASSISTANT", assistantId);
            boundServerIds = boundTools.stream()
                    .filter(t -> t.getMcpServerId() != null)
                    .map(ToolDefinitionEntity::getMcpServerId)
                    .collect(Collectors.toSet());
        }

        List<McpBindVO> result = new ArrayList<>();
        for (OwnerToolBindingContext.McpServerWithTools item : allData) {
            McpServerEntity server = item.mcpServer();
            McpBindVO vo = new McpBindVO();
            vo.setMcpServerId(server.getId());
            vo.setMcpServerName(server.getName());
            vo.setTransportType(server.getTransportType());
            vo.setEnabled(server.getEnabled() == 1);
            vo.setBound(boundServerIds.contains(server.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateMcpBindings(Long assistantId, List<Long> mcpServerIds) {
        // 校验助手存在（走缓存）
        if (assistantContext.getAssistantById(assistantId) == null) {
            throw new NotFoundException("助手不存在: " + assistantId);
        }

        // 校验 MCP Server 存在性（走缓存）
        if (mcpServerIds != null) {
            for (Long mcpId : mcpServerIds) {
                if (ownerToolBindingContext.getMcpServer(mcpId) == null) {
                    throw new BadRequestException("MCP 服务不存在: " + mcpId);
                }
            }
        }

        bindMcpServerTools(RoleType.ASSISTANT, assistantId, mcpServerIds);
    }

    /**
     * 绑定指定 MCP Server 下的所有工具到 Owner
     * <p>
     * 全量替换：先清旧绑定，再插新绑定。完成后刷新 OwnerToolBindingContext 缓存。
     */
    private void bindMcpServerTools(RoleType ownerType, Long ownerId, List<Long> mcpServerIds) {
        // 先清除旧的绑定
        ownerToolBindingDao.deleteByOwner(ownerType, ownerId);

        if (mcpServerIds != null && !mcpServerIds.isEmpty()) {
            // 查询这些 MCP Server 下的所有工具 ID
            List<ToolDefinitionEntity> tools = toolDefinitionDao.findByMcpServerIds(mcpServerIds);
            List<Long> toolIds = tools.stream()
                    .map(ToolDefinitionEntity::getId)
                    .collect(Collectors.toList());

            // 批量插入
            ownerToolBindingDao.batchInsert(ownerType, ownerId, toolIds);
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
}
