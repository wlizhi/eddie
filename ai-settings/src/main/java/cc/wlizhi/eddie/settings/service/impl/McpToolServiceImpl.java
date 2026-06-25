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
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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

        // 1. MCP 存在性校验（走缓存）
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(mcpServerId);
        if (mcp == null) {
            throw new NotFoundException("MCP 服务不存在: " + mcpServerId);
        }

        // 2. 更新工具级别状态
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            for (McpStatusUpdateRequest.ToolStatusItem item : request.getTools()) {
                toolDefinitionDao.updateEnabled(item.getId(), item.getEnabled() ? 1 : 0);
            }
        }

        // 3. 更新 MCP 级别状态
        if (request.getMcpEnabled() != null) {
            mcpServerDao.updateEnabled(mcpServerId, request.getMcpEnabled() ? 1 : 0);
        }

        // 4. 级联校验：查询该 MCP 下所有工具的 enabled 状态（从 DB 查实时数据）
        List<ToolDefinitionEntity> allTools = toolDefinitionDao.findByMcpServerId(mcpServerId);
        boolean anyEnabled = allTools.stream().anyMatch(t -> t.getEnabled() == 1);
        mcpServerDao.updateEnabled(mcpServerId, anyEnabled ? 1 : 0);

        // 5. 刷新缓存
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
     */
    private void checkNameUnique(String name) {
        List<OwnerToolBindingContext.McpServerWithTools> allData = ownerToolBindingContext.getAllMcpServersWithTools();
        boolean exists = allData.stream()
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
