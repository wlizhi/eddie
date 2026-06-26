package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.ToolType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.ConflictException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
import cc.wlizhi.eddie.settings.entity.response.McpToolItemVO;
import cc.wlizhi.eddie.settings.service.McpToolService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 工具管理业务实现
 * <p>
 * 核心原则：
 * <ul>
 *   <li>查询走缓存（OwnerToolBindingContext）</li>
 *   <li>修改后刷新缓存</li>
 *   <li>不使用子查询和表关联</li>
 * </ul>
 */
@Slf4j
@Service
public class McpToolServiceImpl implements McpToolService {

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private McpServerDao mcpServerDao;

    @Resource
    private ToolDefinitionDao toolDefinitionDao;

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Override
    public List<McpServerVO> listAll() {
        // 走缓存：获取全量 MCP + 工具二层结构
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();
        List<McpServerVO> result = new ArrayList<>(allData.size());
        for (OwnerToolBindingContext.McpServerWithTools item : allData) {
            result.add(toMcpServerVO(item.mcpServer(), item.tools()));
        }
        return result;
    }

    @Override
    @Transactional
    public McpServerVO create(McpServerCreateRequest request) {
        // 1. 参数校验
        validateCreateRequest(request);

        // 2. 名称唯一性校验（走缓存）
        checkNameUnique(request.getName());

        // 3. 构建实体
        McpServerEntity entity = new McpServerEntity();
        entity.setName(request.getName().trim());
        entity.setTransportType(request.getTransportType());
        entity.setCommand(request.getCommand() != null ? request.getCommand() : "");
        entity.setArgs(request.getArgs() != null ? request.getArgs() : "[]");
        entity.setEnv(request.getEnv() != null ? request.getEnv() : "{}");
        entity.setUrl(request.getUrl() != null ? request.getUrl() : "");
        entity.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60);
        entity.setEnabled(1);
        entity.setBuiltIn(0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        // 4. 插入 DB
        try {
            mcpServerDao.insert(entity);
        } catch (UncategorizedSQLException ex) {
            throw new ConflictException("MCP 服务名称已存在: " + entity.getName());
        }

        // 5. 获取自增 ID
        Long mcpServerId = mcpServerDao.findLastInsertId();

        // 6. 自动扫描工具（TODO: 后续接入 MCP SDK listTools）
        // 当前版本创建时不同步扫描工具，用户可手动添加或由 MCP 客户端启动时自动注册
        // 预留：List<ToolDefinitionEntity> scannedTools = mcpClient.listTools(mcpServerId);
        // toolDefinitionDao.batchInsert(scannedTools);

        // 7. 刷新缓存
        ownerToolBindingContext.refresh();

        // 8. 返回完整 VO
        McpServerEntity saved = mcpServerDao.findById(mcpServerId).orElse(null);
        List<ToolDefinitionEntity> tools = toolDefinitionDao.findByMcpServerId(mcpServerId);
        return toMcpServerVO(saved, tools);
    }

    @Override
    @Transactional
    public void updateStatus(McpStatusUpdateRequest request) {
        Long mcpServerId = request.getMcpServerId();

        // 1. 从缓存获取 MCP 服务及全量工具列表（不区分启用/禁用状态）
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(mcpServerId);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + mcpServerId);
        }
        List<ToolDefinitionEntity> allTools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
        Map<Long, ToolDefinitionEntity> toolMap = allTools.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getId, t -> t));

        // 2. 内存计算目标状态（完全基于缓存，不查 DB）
        Boolean mcpEnabledParam = request.getMcpEnabled();
        boolean targetMcpEnabled;
        // toolId → 目标 enabled 值 (0/1)
        Map<Long, Integer> targetToolMap = new HashMap<>();

        if (mcpEnabledParam != null) {
            // 场景 A: MCP 纬度显式传递 → 以 MCP 为准，覆盖该 MCP 下所有工具为一致状态
            log.info("MCP状态更新 - MCP纬度覆盖: mcpServerId={}, mcpEnabled={}",
                    mcpServerId, mcpEnabledParam);
            targetMcpEnabled = mcpEnabledParam;
            int enabledInt = targetMcpEnabled ? 1 : 0;
            for (ToolDefinitionEntity tool : allTools) {
                targetToolMap.put(tool.getId(), enabledInt);
            }
        } else {
            // 场景 B: MCP 纬度未传递 → 仅更新工具纬度，推导 MCP 状态
            if (request.getTools() != null) {
                for (McpStatusUpdateRequest.ToolStatusItem item : request.getTools()) {
                    // 校验工具归属合法性
                    ToolDefinitionEntity tool = toolMap.get(item.getId());
                    if (tool == null) {
                        throw new BadRequestException("工具不存在或不属于该 MCP: " + item.getId());
                    }
                    targetToolMap.put(item.getId(), item.getEnabled() ? 1 : 0);
                }
            }

            // 推导 MCP 状态：遍历所有工具，未在 targetToolMap 中的保持原状态
            boolean anyEnabled = false;
            for (ToolDefinitionEntity tool : allTools) {
                Integer target = targetToolMap.get(tool.getId());
                int finalState = target != null ? target : tool.getEnabled();
                if (finalState == 1) {
                    anyEnabled = true;
                    break;
                }
            }
            targetMcpEnabled = anyEnabled;
            log.info("MCP状态更新 - 工具纬度推导: mcpServerId={}, targetMcpEnabled={}",
                    mcpServerId, targetMcpEnabled);
        }

        // 3. 差异比对 + 增量持久化（仅持久化有变更的项）
        // 3a. MCP 级别
        if (mcp.getEnabled() != (targetMcpEnabled ? 1 : 0)) {
            mcpServerDao.updateEnabled(mcpServerId, targetMcpEnabled ? 1 : 0);
        }

        // 3b. 工具级别
        for (Map.Entry<Long, Integer> entry : targetToolMap.entrySet()) {
            Long toolId = entry.getKey();
            int newEnabled = entry.getValue();
            ToolDefinitionEntity currentTool = toolMap.get(toolId);
            if (currentTool != null && currentTool.getEnabled() != newEnabled) {
                toolDefinitionDao.updateEnabled(toolId, newEnabled);
            }
        }

        // 4. 刷新上下文缓存
        ownerToolBindingContext.refresh();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 1. 存在性 + builtIn 校验（走缓存）
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(id);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + id);
        }
        if (mcp.getBuiltIn() == 1) {
            throw new BadRequestException("内置 MCP 服务不可删除");
        }

        // 2. 查询该 MCP 下所有工具 ID（两步法，不用子查询）
        List<ToolDefinitionEntity> tools = toolDefinitionDao.findByMcpServerId(id);
        List<Long> toolIds = tools.stream()
                .map(ToolDefinitionEntity::getId)
                .collect(Collectors.toList());

        // 3. 级联删除：先删绑定关系，再删工具，最后删 MCP
        if (!toolIds.isEmpty()) {
            ownerToolBindingDao.deleteByToolIds(toolIds);
        }
        toolDefinitionDao.deleteByMcpServerId(id);
        mcpServerDao.deleteById(id);

        // 4. 刷新缓存
        ownerToolBindingContext.refresh();
    }

    // ==================== 内部方法 ====================

    /**
     * 校验新增请求的参数完整性
     */
    private void validateCreateRequest(McpServerCreateRequest request) {
        String transportType = request.getTransportType();
        if ("STDIO".equalsIgnoreCase(transportType)) {
            if (request.getCommand() == null || request.getCommand().isBlank()) {
                throw new BadRequestException("STDIO 模式下 command 不能为空");
            }
        } else if ("SSE".equalsIgnoreCase(transportType)
                || "STREAMABLE_HTTP".equalsIgnoreCase(transportType)) {
            if (request.getUrl() == null || request.getUrl().isBlank()) {
                throw new BadRequestException("SSE/HTTP 模式下 url 不能为空");
            }
        }
    }

    /**
     * 校验名称唯一性（走缓存）
     * <p>
     * 仅检查用户自定义的 MCP（builtIn=0），内置 MCP 允许用户创建同名副本进行自定义。
     */
    private void checkNameUnique(String name) {
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();
        boolean exists = allData.stream()
                .filter(item -> item.mcpServer().getBuiltIn() == 0)
                .anyMatch(item -> item.mcpServer().getName().equalsIgnoreCase(name.trim()));
        if (exists) {
            throw new ConflictException("MCP 服务名称已存在: " + name.trim());
        }
    }

    /**
     * McpServerEntity + Tool列表 → McpServerVO
     */
    private McpServerVO toMcpServerVO(McpServerEntity entity, List<ToolDefinitionEntity> tools) {
        if (entity == null) return null;
        McpServerVO vo = new McpServerVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setTransportType(entity.getTransportType());
        vo.setCommand(entity.getCommand());
        vo.setArgs(entity.getArgs());
        vo.setEnv(entity.getEnv());
        vo.setUrl(entity.getUrl());
        vo.setTimeoutSeconds(entity.getTimeoutSeconds());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setBuiltIn(entity.getBuiltIn() == 1);
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (tools != null) {
            vo.setTools(tools.stream().map(this::toMcpToolItemVO).collect(Collectors.toList()));
        } else {
            vo.setTools(List.of());
        }
        return vo;
    }

    /**
     * ToolDefinitionEntity → McpToolItemVO
     */
    private McpToolItemVO toMcpToolItemVO(ToolDefinitionEntity entity) {
        if (entity == null) return null;
        McpToolItemVO vo = new McpToolItemVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDisplayName(entity.getDisplayName());
        vo.setDescription(entity.getDescription());
        vo.setToolType(entity.getToolType() != null ? entity.getToolType().name() : ToolType.MCP.name());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setBuiltIn(entity.getBuiltIn() == 1);
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }
}
