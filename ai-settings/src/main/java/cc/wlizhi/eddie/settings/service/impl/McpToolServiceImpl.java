/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.dto.ConfigSchema;
import cc.wlizhi.eddie.common.dto.McpConnectInfo;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.McpSourceType;
import cc.wlizhi.eddie.common.enums.McpTransportType;
import cc.wlizhi.eddie.common.enums.ToolType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.settings.entity.request.BuiltInStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpConnectResult;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
import cc.wlizhi.eddie.settings.entity.response.McpToolItemVO;
import cc.wlizhi.eddie.settings.service.McpToolService;
import cc.wlizhi.eddie.tools.service.McpClientHolder;
import cc.wlizhi.eddie.tools.service.McpClientRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
    @Resource
    private InitScheduler initScheduler;

    @Resource
    private List<BuiltInToolProvider> toolProviders;

    /**
     * 注册重连成功回调：当 MCP 后台重连成功时自动同步工具列表到 DB
     */
    @PostConstruct
    public void registerReconnectCallback() {
        initScheduler.addTask(this.getClass().getSimpleName(), 100, this::doRegisterReconnectCallback);
    }

    private void doRegisterReconnectCallback() {
        mcpClientRegistry.addReconnectCallback((mcpServerId, callbacks) -> {
            try {
                List<McpConnectInfo.ToolInfo> toolInfos = callbacks.stream()
                        .map(cb -> {
                            var def = cb.getToolDefinition();
                            return new McpConnectInfo.ToolInfo(
                                    cb.getOriginalToolName(), def.description(), def.inputSchema());
                        }).collect(Collectors.toList());
                syncRemoteToolsToDb(mcpServerId, toolInfos);
                ownerToolBindingContext.refresh();
                log.info("MCP 重连成功自动同步工具: id={}, count={}", mcpServerId, toolInfos.size());
            } catch (Exception e) {
                log.error("MCP 重连同步工具失败: id={}", mcpServerId, e);
            }
        });
    }

    @Override
    public List<McpServerVO> listAll() {
        // 走缓存：获取全量 MCP + 工具二层结构
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();
        List<McpServerVO> result = new ArrayList<>(allData.size());
        for (OwnerToolBindingContext.McpServerWithTools item : allData) {
            result.add(toMcpServerVO(item.getMcpServer(), item.getTools()));
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
                    .map(item -> toMcpServerVO(item.getMcpServer(), item.getTools()))
                    .collect(Collectors.toList());
        }
        // enabled = false → 从全量中过滤出已禁用的 MCP
        List<OwnerToolBindingContext.McpServerWithTools> allData =
                ownerToolBindingContext.getAllMcpServersWithTools();
        return allData.stream()
                .map(item -> toMcpServerVO(item.getMcpServer(), item.getTools()))
                .collect(Collectors.toList());
    }

    @Override
    public McpServerVO create(McpServerCreateRequest request) {
        // 1. 参数校验
        validateCreateRequest(request);

        // 2. 构建实体（不校验名称唯一性）
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
        boolean enabled = request.getEnabled() != null && request.getEnabled();
        entity.setEnabled(enabled ? 1 : 0);
        entity.setSourceType("USER");
        entity.setSourceConfig("{}");
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setReconnectIntervalSec(request.getReconnectIntervalSec());
        entity.setMaxReconnectAttempts(request.getMaxReconnectAttempts());

        // 3. 插入 DB
        mcpServerDao.insert(entity);
        Long mcpServerId = mcpServerDao.findLastInsertId();

        // 4. 仅启用时才注册 MCP 客户端连接并自动扫描工具
        McpServerEntity saved = mcpServerDao.findById(mcpServerId).orElse(null);
        List<ToolDefinitionEntity> tools = List.of();
        if (saved != null && enabled) {
            // 同步连接 MCP 并获取远端工具列表
            McpConnectInfo connectInfo = mcpClientRegistry.registerSync(saved);
            if (connectInfo.isConnected()) {
                // 连接成功 → 同步远端工具到 DB
                tools = syncRemoteToolsToDb(mcpServerId, connectInfo.getTools());
                log.info("MCP 创建成功并自动扫描工具: name={}, toolCount={}",
                        saved.getName(), tools.size());
            } else {
                // 连接失败 → 降级为 register 启动自动重连，不同步工具
                log.warn("MCP 创建时连接失败，将启动自动重连: name={}, error={}",
                        saved.getName(), connectInfo.getMessage());
                mcpClientRegistry.register(saved);
            }
        }

        // 5. 刷新缓存
        ownerToolBindingContext.refresh();

        // 6. 返回完整 VO
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
        // BUILT_IN 类型禁止使用此接口
        McpSourceType sourceType = McpSourceType.fromCode(mcp.getSourceType());
        if (sourceType == McpSourceType.BUILT_IN) {
            throw new BadRequestException("内置工具请使用 /built-in/status 接口操作");
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
    public McpServerVO update(McpServerUpdateRequest request) {
        Long id = request.getId();

        // 1. 校验存在
        McpServerEntity existing = mcpServerDao.findById(id)
                .orElseThrow(() -> new NotFoundException("MCP 服务不存在: " + id));

        // 2. 更新字段
        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null ? request.getDescription() : "");
        existing.setSourceType(request.getSourceType() != null ? request.getSourceType() : existing.getSourceType());
        existing.setSourceConfig(request.getSourceConfig() != null ? request.getSourceConfig() : existing.getSourceConfig());
        existing.setTransportType(request.getTransportType());
        existing.setCommand(request.getCommand() != null ? request.getCommand() : "");
        existing.setArgs(request.getArgs() != null ? request.getArgs() : "[]");
        existing.setEnv(request.getEnv() != null ? request.getEnv() : "");
        existing.setUrl(request.getUrl() != null ? request.getUrl() : "");
        existing.setHeaders(request.getHeaders() != null ? request.getHeaders() : "");
        existing.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60);
        existing.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        existing.setReconnectIntervalSec(request.getReconnectIntervalSec());
        existing.setMaxReconnectAttempts(request.getMaxReconnectAttempts());

        boolean targetEnabled = request.getEnabled() != null && request.getEnabled();
        existing.setEnabled(targetEnabled ? 1 : 0);

        // 3. 持久化（无事务：避免远程 MCP 调用占用连接）
        mcpServerDao.update(existing);

        // 4. 根据启禁状态决定操作
        List<ToolDefinitionEntity> tools = List.of();
        if (targetEnabled) {
            McpConnectInfo connectInfo = mcpClientRegistry.registerSync(existing);
            if (connectInfo.isConnected()) {
                tools = syncRemoteToolsToDb(id, connectInfo.getTools());
                log.info("MCP 编辑保存并同步工具: id={}, toolCount={}", id, tools.size());
            } else {
                log.warn("MCP 编辑保存时连接失败，降级自动重连: id={}", id);
                mcpClientRegistry.register(existing);
            }
        } else {
            mcpClientRegistry.unregister(id);
        }

        // 5. 刷新缓存
        ownerToolBindingContext.refresh();

        // 6. 返回完整 VO
        return toMcpServerVO(existing, tools);
    }

    @Override
    public void updateBuiltInStatus(BuiltInStatusUpdateRequest request) {
        Boolean enabled = request.getEnabled();
        if (enabled == null) {
            throw new BadRequestException("启用状态不能为空");
        }

        if (request.getMcpServerId() != null) {
            // 模式 A: MCP 级别切换 — 批量更新该 MCP 下所有工具 + MCP 自身状态
            Long mcpServerId = request.getMcpServerId();
            List<ToolDefinitionEntity> tools = toolDefinitionDao.findByMcpServerId(mcpServerId);
            for (ToolDefinitionEntity tool : tools) {
                toolDefinitionDao.updateEnabled(tool.getId(), enabled ? 1 : 0);
            }
            // 同步更新 MCP 服务器自身的 enabled 字段
            mcpServerDao.updateEnabled(mcpServerId, enabled ? 1 : 0);
            log.info("内置工具 MCP 级别批量更新: mcpServerId={}, toolCount={}, enabled={}",
                    mcpServerId, tools.size(), enabled);
        } else if (request.getToolId() != null) {
            // 模式 B: 工具级别切换 — 更新单个工具，并联动 MCP 状态
            ToolDefinitionEntity tool = toolDefinitionDao.findById(request.getToolId());
            if (tool == null || tool.getBuiltIn() != 1) {
                throw new BadRequestException("工具不存在或非内置工具");
            }
            toolDefinitionDao.updateEnabled(request.getToolId(), enabled ? 1 : 0);
            log.info("内置工具状态已更新: toolId={}, enabled={}", request.getToolId(), enabled);

            // 联动 MCP 状态：查询同 MCP 下所有工具，全部禁用则 MCP 禁用，否则 MCP 启用
            if (tool.getMcpServerId() != null) {
                List<ToolDefinitionEntity> siblings = toolDefinitionDao.findByMcpServerId(tool.getMcpServerId());
                boolean anyEnabled = siblings.stream().anyMatch(t ->
                        t.getId().equals(request.getToolId()) ? enabled : t.getEnabled() == 1);
                McpServerEntity mcp = mcpServerDao.findById(tool.getMcpServerId()).orElse(null);
                if (mcp != null) {
                    int newMcpEnabled = anyEnabled ? 1 : 0;
                    if (mcp.getEnabled() != newMcpEnabled) {
                        mcpServerDao.updateEnabled(tool.getMcpServerId(), newMcpEnabled);
                        log.info("内置工具联动 MCP 状态: mcpServerId={}, enabled={}",
                                tool.getMcpServerId(), newMcpEnabled);
                    }
                }
            }
        } else {
            throw new BadRequestException("mcpServerId 和 toolId 不能同时为空");
        }

        // 刷新缓存
        ownerToolBindingContext.refresh();
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
                    vo.setDisplayName(t.getName());
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

        // 填充连接状态（仅 enabled 时有意义）
        String sourceType = entity.getSourceType();
        if (Objects.equals(sourceType, McpSourceType.BUILT_IN.name())) {
            vo.setConnectionStatus(McpClientHolder.ConnectionState.CONNECTED.name());

            // BUILT_IN 类型：填充 sourceConfig 和 configSchema
            vo.setSourceConfig(entity.getSourceConfig());
            ConfigSchema schema = findConfigSchema(entity.getName());
            vo.setConfigSchema(schema);
        } else if (entity.getEnabled() == 1) {
            vo.setConnectionStatus(mcpClientRegistry.getConnectionState(entity.getId()).name());
        } else {
            vo.setConnectionStatus("DISCONNECTED");
        }

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
        vo.setEnabledStatus(entity.getEnabled());
        vo.setBuiltIn(entity.getBuiltIn() == 1);
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    /**
     * 根据 MCP Server 名称查找对应内置工具的 Config Schema。
     * 遍历所有 {@link BuiltInToolProvider}，匹配 serverName 返回其配置描述。
     */
    private ConfigSchema findConfigSchema(String serverName) {
        if (serverName == null || toolProviders == null) {
            return ConfigSchema.empty();
        }
        for (BuiltInToolProvider provider : toolProviders) {
            if (serverName.equals(provider.getMcpServerName())) {
                return provider.getConfigSchema();
            }
        }
        return ConfigSchema.empty();
    }
}
