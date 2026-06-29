package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.dto.McpConnectInfo;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.McpSourceType;
import cc.wlizhi.eddie.common.enums.McpTransportType;
import cc.wlizhi.eddie.common.enums.ToolType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.ConflictException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpConnectResult;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
import cc.wlizhi.eddie.settings.entity.response.McpToolItemVO;
import cc.wlizhi.eddie.settings.service.McpToolService;
import cc.wlizhi.eddie.tools.service.McpClientRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    @Resource
    private McpClientRegistry mcpClientRegistry;

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
    public List<McpServerVO> listAll(Boolean enabled) {
        if (enabled == null) {
            // 不传 → 全量
            return listAll();
        }
        if (enabled) {
            // 仅已启用的 MCP（MCP 和工具均需启用）
            List<OwnerToolBindingContext.McpServerWithTools> enabledData =
                    ownerToolBindingContext.getEnabledMcpServersWithTools();
            return enabledData.stream()
                    .map(item -> toMcpServerVO(item.mcpServer(), item.tools()))
                    .collect(Collectors.toList());
        }
        // enabled = false → 从全量中过滤出已禁用的 MCP
        List<OwnerToolBindingContext.McpServerWithTools> allData =
                ownerToolBindingContext.getAllMcpServersWithTools();
        return allData.stream()
                .filter(item -> item.mcpServer().getEnabled() != 1)
                .map(item -> toMcpServerVO(item.mcpServer(), item.tools()))
                .collect(Collectors.toList());
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
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setTransportType(request.getTransportType());
        entity.setCommand(request.getCommand() != null ? request.getCommand() : "");
        entity.setArgs(request.getArgs() != null ? request.getArgs() : "[]");
        entity.setEnv(request.getEnv() != null ? request.getEnv() : "");
        entity.setUrl(request.getUrl() != null ? request.getUrl() : "");
        entity.setHeaders(request.getHeaders() != null ? request.getHeaders() : "");
        entity.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60);
        entity.setEnabled(1);
        entity.setSourceType("USER");
        entity.setSourceConfig("{}");
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setReconnectIntervalSec(request.getReconnectIntervalSec());
        entity.setMaxReconnectAttempts(request.getMaxReconnectAttempts());

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

        // 8. 注册 MCP 客户端连接
        McpServerEntity saved = mcpServerDao.findById(mcpServerId).orElse(null);
        if (saved != null) {
            mcpClientRegistry.register(saved);
        }

        // 9. 返回完整 VO
        List<ToolDefinitionEntity> tools = toolDefinitionDao.findByMcpServerId(mcpServerId);
        return toMcpServerVO(saved, tools);
    }

    @Override
    @Transactional
    public McpConnectResult updateStatus(McpStatusUpdateRequest request) {
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

        // 5. 如果启用了 MCP → 同步连接并扫描远端工具
        McpConnectResult connectResult = null;
        if (targetMcpEnabled) {
            // 重新从 DB 获取最新的 MCP 配置（状态已更新）
            McpServerEntity server = mcpServerDao.findById(mcpServerId).orElse(null);
            if (server != null) {
                connectResult = doConnectAndSyncTools(server);
            }
        } else {
            // 禁用 → 断开连接
            mcpClientRegistry.unregister(mcpServerId);
            connectResult = McpConnectResult.success("MCP 已禁用", List.of());
        }

        return connectResult;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 1. 存在性 + sourceType 校验（走缓存）
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(id);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + id);
        }
        McpSourceType sourceType = McpSourceType.fromCode(mcp.getSourceType());
        if (sourceType != null && sourceType.isSystem()) {
            throw new BadRequestException("系统预置 MCP 服务不可删除");
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

        // 4. 断开 MCP 客户端连接
        mcpClientRegistry.unregister(id);

        // 5. 刷新缓存
        ownerToolBindingContext.refresh();
    }

    @Override
    public List<McpToolItemVO> listToolsByMcpServer(Long mcpServerId) {
        // 走缓存查询
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(mcpServerId);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + mcpServerId);
        }
        List<ToolDefinitionEntity> tools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
        return tools.stream().map(this::toMcpToolItemVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public McpConnectResult syncTools(Long mcpServerId) {
        // 1. 校验 MCP 服务存在
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(mcpServerId);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + mcpServerId);
        }

        // 2. 重新连接 MCP 并获取远端工具列表
        McpServerEntity server = mcpServerDao.findById(mcpServerId).orElse(null);
        if (server == null) {
            return McpConnectResult.failure("MCP 服务数据不存在");
        }

        // 3. 同步连接
        McpConnectInfo connectInfo = mcpClientRegistry.registerSync(server);
        if (!connectInfo.isConnected()) {
            // DB 状态改为禁用（连接失败）
            mcpServerDao.updateEnabled(mcpServerId, 0);
            ownerToolBindingContext.refresh();
            return McpConnectResult.failure(connectInfo.getMessage());
        }

        // 4. 同步远端工具到 DB
        List<ToolDefinitionEntity> syncedTools = syncRemoteToolsToDb(mcpServerId, connectInfo.getTools());

        // 5. 刷新缓存
        ownerToolBindingContext.refresh();

        // 6. 组装返回
        List<McpToolItemVO> toolVOs = syncedTools.stream()
                .map(this::toMcpToolItemVO)
                .collect(Collectors.toList());
        return McpConnectResult.success(
                "同步成功，共 " + toolVOs.size() + " 个工具", toolVOs);
    }

    @Override
    public McpConnectResult testConnection(McpServerCreateRequest request) {
        // 1. 从请求构建临时 McpServerEntity（不含 id，仅连接参数）
        McpServerEntity tempEntity = new McpServerEntity();
        tempEntity.setName(request.getName() != null ? request.getName() : "test");
        tempEntity.setTransportType(request.getTransportType());
        tempEntity.setCommand(request.getCommand() != null ? request.getCommand() : "");
        tempEntity.setArgs(request.getArgs() != null ? request.getArgs() : "[]");
        tempEntity.setEnv(request.getEnv() != null ? request.getEnv() : "");
        tempEntity.setUrl(request.getUrl() != null ? request.getUrl() : "");
        tempEntity.setHeaders(request.getHeaders() != null ? request.getHeaders() : "");
        tempEntity.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60);

        // 2. 调用 MCP 客户端测试连接（不注册、不写 DB、不改变状态）
        McpConnectInfo connectInfo = mcpClientRegistry.testConnection(tempEntity);
        if (!connectInfo.isConnected()) {
            log.warn("MCP 连接测试失败: name={}, error={}",
                    tempEntity.getName(), connectInfo.getMessage());
            return McpConnectResult.failure(connectInfo.getMessage());
        }

        // 3. 连接成功，将远端工具转成 VO 返回
        List<McpToolItemVO> toolVOs = connectInfo.getTools().stream()
                .map(t -> {
                    McpToolItemVO vo = new McpToolItemVO();
                    vo.setName(t.getName());
                    vo.setDisplayName(t.getDescription() != null && !t.getDescription().isBlank()
                            ? t.getDescription() : t.getName());
                    vo.setDescription(t.getDescription());
                    vo.setToolType(ToolType.MCP.name());
                    return vo;
                })
                .collect(Collectors.toList());

        log.info("MCP 连接测试成功: name={}, tools={}",
                tempEntity.getName(), toolVOs.size());
        return McpConnectResult.success(
                "连接成功，检测到 " + toolVOs.size() + " 个工具", toolVOs);
    }

    // ==================== 内部方法 ====================

    /**
     * 连接 MCP 服务器并同步远端工具到 DB
     * <p>
     * 在启用 MCP 时调用，同时处理连接结果和工具入库。
     */
    private McpConnectResult doConnectAndSyncTools(McpServerEntity server) {
        Long mcpServerId = server.getId();

        // 1. 同步连接 MCP 协议
        McpConnectInfo connectInfo = mcpClientRegistry.registerSync(server);
        if (!connectInfo.isConnected()) {
            // 连接失败 → DB 状态回滚为禁用
            log.warn("MCP 启用连接失败，回滚禁用状态: id={}, name={}, error={}",
                    mcpServerId, server.getName(), connectInfo.getMessage());
            mcpServerDao.updateEnabled(mcpServerId, 0);
            ownerToolBindingContext.refresh();
            return McpConnectResult.failure(connectInfo.getMessage());
        }

        // 2. 连接成功 → 同步远端工具到 DB
        List<ToolDefinitionEntity> syncedTools = syncRemoteToolsToDb(mcpServerId, connectInfo.getTools());

        // 3. 刷新缓存
        ownerToolBindingContext.refresh();

        // 4. 组装返回
        List<McpToolItemVO> toolVOs = syncedTools.stream()
                .map(this::toMcpToolItemVO)
                .collect(Collectors.toList());
        return McpConnectResult.success(
                "连接成功，已同步 " + toolVOs.size() + " 个工具", toolVOs);
    }

    /**
     * 将 MCP 远端工具列表同步到 DB（全量覆盖策略）
     * <p>
     * 逻辑：按 name 去重，新增不在 DB 中的工具，删除 MCP 不再提供的工具。
     * 已存在的工具保持原来的 enabled / sortOrder 不变。
     *
     * @param mcpServerId MCP 服务器 ID
     * @param remoteTools MCP 协议返回的远端工具列表
     * @return 同步后的工具定义列表
     */
    private List<ToolDefinitionEntity> syncRemoteToolsToDb(
            Long mcpServerId, List<McpConnectInfo.ToolInfo> remoteTools) {

        // 1. 查询 DB 中该 MCP 下已有的工具（按 name 索引）
        List<ToolDefinitionEntity> existingTools = toolDefinitionDao.findByMcpServerId(mcpServerId);
        Map<String, ToolDefinitionEntity> existingByName = new HashMap<>();
        for (ToolDefinitionEntity t : existingTools) {
            existingByName.put(t.getName(), t);
        }
        Set<String> remoteNames = new HashSet<>();

        // 2. 遍历远端工具，新增或更新
        List<ToolDefinitionEntity> toInsert = new ArrayList<>();
        int sortOrder = 0;
        for (McpConnectInfo.ToolInfo remote : remoteTools) {
            remoteNames.add(remote.getName());
            ToolDefinitionEntity existing = existingByName.get(remote.getName());
            if (existing != null) {
                // 已存在 → 更新描述等非关键字段（保持 enabled/sortOrder）
                boolean changed = false;
                if (!remote.getDescription().equals(existing.getDescription())) {
                    existing.setDescription(remote.getDescription());
                    changed = true;
                }
                String displayName = remote.getDescription() != null && !remote.getDescription().isBlank()
                        ? remote.getDescription() : remote.getName();
                if (!displayName.equals(existing.getDisplayName())) {
                    existing.setDisplayName(displayName);
                    changed = true;
                }
                if (changed) {
                    toolDefinitionDao.update(existing);
                }
            } else {
                // 不存在 → 新增
                ToolDefinitionEntity entity = new ToolDefinitionEntity();
                entity.setToolType(ToolType.MCP);
                entity.setName(remote.getName());
                entity.setDisplayName(remote.getDescription() != null && !remote.getDescription().isBlank()
                        ? remote.getDescription() : remote.getName());
                entity.setDescription(remote.getDescription());
                entity.setEnabled(1);
                entity.setBuiltIn(0);
                entity.setMcpServerId(mcpServerId);
                entity.setSortOrder(sortOrder);
                toInsert.add(entity);
            }
            sortOrder++;
        }

        // 3. 批量新增
        if (!toInsert.isEmpty()) {
            toolDefinitionDao.batchInsert(toInsert);
            log.info("MCP 同步工具: id={}, 新增 {} 个工具", mcpServerId, toInsert.size());
        }

        // 4. 删除远端不再提供的工具（从 DB 中删除、清理绑定关系）
        List<Long> toDeleteIds = new ArrayList<>();
        for (ToolDefinitionEntity existing : existingTools) {
            if (!remoteNames.contains(existing.getName())) {
                toDeleteIds.add(existing.getId());
            }
        }
        if (!toDeleteIds.isEmpty()) {
            ownerToolBindingDao.deleteByToolIds(toDeleteIds);
            for (Long id : toDeleteIds) {
                toolDefinitionDao.deleteById(id);
            }
            log.info("MCP 同步工具: id={}, 移除 {} 个不再提供的工具", mcpServerId, toDeleteIds.size());
        }

        // 5. 重新查询全量返回
        return toolDefinitionDao.findByMcpServerId(mcpServerId);
    }

    /**
     * 校验新增请求的参数完整性
     */
    private void validateCreateRequest(McpServerCreateRequest request) {
        McpTransportType transportType = McpTransportType.fromCode(request.getTransportType());
        if (transportType == null) {
            throw new BadRequestException("不支持的传输类型: " + request.getTransportType());
        }
        switch (transportType) {
            case STDIO -> {
                if (request.getCommand() == null || request.getCommand().isBlank()) {
                    throw new BadRequestException("STDIO 模式下 command 不能为空");
                }
            }
            case SSE, STREAMABLE_HTTP -> {
                if (request.getUrl() == null || request.getUrl().isBlank()) {
                    throw new BadRequestException("SSE/HTTP 模式下 url 不能为空");
                }
            }
        }
    }

    /**
     * 校验名称唯一性（走缓存）
     * <p>
     * 仅检查用户自定义的 MCP（USER 类型），内置工具和第三方服务商允许用户创建同名副本进行自定义。
     */
    private void checkNameUnique(String name) {
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();
        boolean exists = allData.stream()
                .filter(item -> McpSourceType.fromCode(item.mcpServer().getSourceType()) == McpSourceType.USER)
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
        vo.setDescription(entity.getDescription());
        vo.setTransportType(entity.getTransportType());
        vo.setCommand(entity.getCommand());
        vo.setArgs(entity.getArgs());
        vo.setEnv(entity.getEnv());
        vo.setUrl(entity.getUrl());
        vo.setHeaders(entity.getHeaders());
        vo.setTimeoutSeconds(entity.getTimeoutSeconds());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setSourceType(McpSourceType.fromCode(entity.getSourceType()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setReconnectIntervalSec(entity.getReconnectIntervalSec());
        vo.setMaxReconnectAttempts(entity.getMaxReconnectAttempts());
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
