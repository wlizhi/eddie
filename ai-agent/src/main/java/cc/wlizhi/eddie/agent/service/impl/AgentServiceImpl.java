/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.context.AgentContext;
import cc.wlizhi.eddie.agent.dao.AgentDao;
import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.dao.AgentMsgSegmentDao;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.request.AgentCreateRequest;
import cc.wlizhi.eddie.agent.entity.request.AgentUpdateRequest;
import cc.wlizhi.eddie.agent.entity.response.AgentDetailVO;
import cc.wlizhi.eddie.agent.entity.response.AgentVO;
import cc.wlizhi.eddie.agent.service.AgentService;
import cc.wlizhi.eddie.chat.entity.response.ToolItemVO;
import cc.wlizhi.eddie.chat.entity.response.ToolSourceVO;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体管理业务实现
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private AgentDao agentDao;

    @Resource
    private AgentSessionDao agentSessionDao;

    @Resource
    private AgentMsgDao agentMsgDao;

    @Resource
    private AgentMsgSegmentDao agentMsgSegmentDao;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AgentContext agentContext;

    @Override
    public List<AgentVO> list(boolean showAll) {
        List<AgentEntity> entities = agentDao.findAll(showAll);
        List<AgentVO> result = new ArrayList<>(entities.size());
        for (AgentEntity entity : entities) {
            AgentVO vo = toVO(entity);
            if (entity.getMainProviderId() != null) {
                ModelProviderEntity provider = modelProviderContext.getModelProviderById(entity.getMainProviderId());
                if (provider != null) {
                    vo.setMainProviderName(provider.getName());
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public AgentDetailVO getDetail(Long id) {
        AgentEntity entity = agentDao.findById(id);
        if (entity == null) {
            throw new NotFoundException("智能体不存在: " + id);
        }
        AgentDetailVO vo = toDetailVO(entity);

        if (entity.getMainProviderId() != null) {
            ModelProviderEntity mainProvider = modelProviderContext.getModelProviderById(entity.getMainProviderId());
            if (mainProvider != null) {
                vo.setMainProviderCode(mainProvider.getCode());
                vo.setMainProviderName(mainProvider.getName());
            }
        }

        if (entity.getSubProviderId() != null) {
            ModelProviderEntity subProvider = modelProviderContext.getModelProviderById(entity.getSubProviderId());
            if (subProvider != null) {
                vo.setSubProviderCode(subProvider.getCode());
                vo.setSubProviderName(subProvider.getName());
            }
        }

        return vo;
    }

    @Override
    @Transactional
    public AgentVO create(AgentCreateRequest request) {
        AgentEntity entity = new AgentEntity();
        entity.setName(request.getName());
        entity.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");

        entity.setMainProviderId(request.getMainProviderId());
        entity.setMainModelId(request.getMainModelId() == null ? "" : request.getMainModelId());
        entity.setMainModelParams(request.getMainModelParams() != null ? request.getMainModelParams() : "{}");

        entity.setSubProviderId(request.getSubProviderId());
        entity.setSubModelId(request.getSubModelId() != null ? request.getSubModelId() : "");
        entity.setSubModelParams(request.getSubModelParams() != null ? request.getSubModelParams() : "{}");

        entity.setSemaphore(request.getSemaphore() != null ? request.getSemaphore() : 1);
        entity.setMaxIterations(request.getMaxIterations() != null ? request.getMaxIterations() : 100);
        entity.setMaxExecutionTimeSec(request.getMaxExecutionTimeSec() != null ? request.getMaxExecutionTimeSec() : 300);
        entity.setExecutionMode(request.getExecutionMode() != null ? request.getExecutionMode() : "FOREGROUND");

        entity.setToolSelectionMode(request.getToolSelectionMode() != null ? request.getToolSelectionMode() : "auto");

        entity.setPreferences(serializePreferences(request.getPreferences()));

        entity.setEnabled(1);
        entity.setBuiltIn(0);
        entity.setSortOrder(0);

        agentDao.insert(entity);
        Long agentId = agentDao.findLastInsertId();

        bindMcpServerTools(agentId, request.getEnabledMcpServerIds());

        AgentEntity saved = agentDao.findById(agentId);
        log.info("创建智能体: id={}, name={}", agentId, saved.getName());
        agentContext.refresh();
        return toVO(saved);
    }

    @Override
    @Transactional
    public AgentVO update(Long id, AgentUpdateRequest request) {
        if (!agentDao.existsById(id)) {
            throw new NotFoundException("智能体不存在: " + id);
        }

        AgentEntity entity = new AgentEntity();
        entity.setId(id);
        entity.setName(request.getName());
        entity.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");

        entity.setMainProviderId(request.getMainProviderId());
        entity.setMainModelId(request.getMainModelId() == null ? "" : request.getMainModelId());
        entity.setMainModelParams(request.getMainModelParams() != null ? request.getMainModelParams() : "{}");

        entity.setSubProviderId(request.getSubProviderId());
        entity.setSubModelId(request.getSubModelId() != null ? request.getSubModelId() : "");
        entity.setSubModelParams(request.getSubModelParams() != null ? request.getSubModelParams() : "{}");

        entity.setSemaphore(request.getSemaphore() != null ? request.getSemaphore() : 1);
        entity.setMaxIterations(request.getMaxIterations() != null ? request.getMaxIterations() : 100);
        entity.setMaxExecutionTimeSec(request.getMaxExecutionTimeSec() != null ? request.getMaxExecutionTimeSec() : 300);
        entity.setExecutionMode(request.getExecutionMode() != null ? request.getExecutionMode() : "FOREGROUND");

        entity.setToolSelectionMode(request.getToolSelectionMode() != null ? request.getToolSelectionMode() : "auto");

        entity.setPreferences(serializePreferences(request.getPreferences()));

        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        entity.setBuiltIn(0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        agentDao.update(entity);

        bindMcpServerTools(id, request.getEnabledMcpServerIds());

        AgentEntity updated = agentDao.findById(id);
        log.info("更新智能体: id={}, name={}", id, updated.getName());
        agentContext.refresh();
        return toVO(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!agentDao.existsById(id)) {
            throw new NotFoundException("智能体不存在: " + id);
        }
        agentMsgSegmentDao.deleteByAgentId(id);
        agentMsgDao.deleteByAgentId(id);
        agentSessionDao.deleteByAgentId(id);
        ownerToolBindingDao.deleteByOwner(RoleType.AGENT, id);
        agentDao.deleteById(id);
        ownerToolBindingContext.refresh();
        agentContext.refresh();
        log.info("删除智能体: id={}", id);
    }

    @Override
    @Transactional
    public void batchSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            agentDao.updateSortOrder(ids.get(i), i + 1);
        }
        agentContext.refresh();
    }

    // ==================== 内部方法 ====================

    private AgentVO toVO(AgentEntity entity) {
        AgentVO vo = new AgentVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setMainProviderId(entity.getMainProviderId());
        vo.setMainModelId(entity.getMainModelId());
        vo.setExecutionMode(entity.getExecutionMode());
        vo.setToolSelectionMode(entity.getToolSelectionMode());
        vo.setEnabled(entity.getEnabled());
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AgentDetailVO toDetailVO(AgentEntity entity) {
        AgentDetailVO vo = new AgentDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());

        vo.setMainProviderId(entity.getMainProviderId());
        vo.setMainModelId(entity.getMainModelId());
        vo.setMainModelParams(entity.getMainModelParams());

        vo.setSubProviderId(entity.getSubProviderId());
        vo.setSubModelId(entity.getSubModelId());
        vo.setSubModelParams(entity.getSubModelParams());

        vo.setSemaphore(entity.getSemaphore());
        vo.setMaxIterations(entity.getMaxIterations());
        vo.setMaxExecutionTimeSec(entity.getMaxExecutionTimeSec());
        vo.setExecutionMode(entity.getExecutionMode());

        vo.setToolSelectionMode(entity.getToolSelectionMode());

        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools("AGENT", entity.getId());
        List<Long> boundIds = boundTools.stream()
                .filter(t -> t.getMcpServerId() != null)
                .map(ToolDefinitionEntity::getMcpServerId)
                .distinct()
                .collect(Collectors.toList());
        vo.setBoundMcpServerIds(boundIds);

        vo.setPreferences(deserializePreferences(entity.getPreferences()));

        vo.setEnabled(entity.getEnabled() == 1);
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setSortOrder(entity.getSortOrder());

        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @Override
    public List<ToolSourceVO> getBoundMcpTools(Long agentId) {
        // 1. 获取智能体绑定的工具列表（已过滤全局启用 + MCP 服务启用）
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(RoleType.AGENT.name(), agentId);
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

    private void bindMcpServerTools(Long ownerId, List<Long> mcpServerIds) {
        ownerToolBindingDao.deleteByOwner(RoleType.AGENT, ownerId);
        if (mcpServerIds != null && !mcpServerIds.isEmpty()) {
            List<Long> toolIds = new ArrayList<>();
            for (Long mcpServerId : mcpServerIds) {
                List<ToolDefinitionEntity> tools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
                if (tools != null) {
                    toolIds.addAll(tools.stream().map(ToolDefinitionEntity::getId).toList());
                }
            }
            if (!toolIds.isEmpty()) {
                ownerToolBindingDao.batchInsert(RoleType.AGENT, ownerId, toolIds);
            }
        }
        ownerToolBindingContext.refresh();
    }

    private String serializePreferences(Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(preferences);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> deserializePreferences(String preferences) {
        if (preferences == null || preferences.isEmpty() || "{}".equals(preferences)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(preferences, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
