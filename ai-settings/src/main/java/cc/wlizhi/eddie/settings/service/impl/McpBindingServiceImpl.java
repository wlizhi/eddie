package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.settings.service.McpBindingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MCP 绑定关系管理实现
 * <p>
 * 以 MCP Server 为粒度，操作 Owner ↔ 工具的绑定关系。
 * 数据校验和查询全部走 OwnerToolBindingContext 缓存，避免查库。
 */
@Slf4j
@Service
public class McpBindingServiceImpl implements McpBindingService {

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Override
    @Transactional
    public void addBinding(RoleType ownerType, Long ownerId, Long mcpServerId) {
        log.info("新增 MCP 绑定: ownerType={}, ownerId={}, mcpServerId={}", ownerType, ownerId, mcpServerId);

        // 1. 从缓存校验 MCP Server 存在
        McpServerEntity mcp = ownerToolBindingContext.getMcpServer(mcpServerId);
        if (mcp == null) {
            throw new BadRequestException("MCP 服务不存在: " + mcpServerId);
        }

        // 2. 从缓存获取该 MCP 下的工具列表
        List<ToolDefinitionEntity> tools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
        if (tools.isEmpty()) {
            log.warn("MCP 服务下无可用工具，跳过绑定: mcpServerId={}, name={}", mcpServerId, mcp.getName());
            return;
        }

        // 3. 提取工具 ID 批量插入绑定表（enabled=1 表示 Owner 启用此工具）
        List<Long> toolIds = tools.stream()
                .map(ToolDefinitionEntity::getId)
                .toList();
        ownerToolBindingDao.batchInsert(ownerType, ownerId, toolIds);
        log.info("MCP 绑定完成: ownerType={}, ownerId={}, mcpServerId={}, toolCount={}",
                ownerType, ownerId, mcpServerId, toolIds.size());

        // 4. 刷新缓存
        ownerToolBindingContext.refresh();
    }

    @Override
    @Transactional
    public void removeBinding(RoleType ownerType, Long ownerId, Long mcpServerId) {
        log.info("移除 MCP 绑定: ownerType={}, ownerId={}, mcpServerId={}", ownerType, ownerId, mcpServerId);

        // 1. 从缓存获取该 MCP 下的工具列表
        List<ToolDefinitionEntity> tools = ownerToolBindingContext.getToolsByMcpServerId(mcpServerId);
        if (tools.isEmpty()) {
            log.warn("MCP 服务下无工具，跳过解绑: mcpServerId={}", mcpServerId);
            return;
        }

        // 2. 提取工具 ID 删除绑定
        List<Long> toolIds = tools.stream()
                .map(ToolDefinitionEntity::getId)
                .toList();
        ownerToolBindingDao.deleteByOwnerAndToolIds(ownerType, ownerId, toolIds);
        log.info("MCP 解绑完成: ownerType={}, ownerId={}, mcpServerId={}, toolCount={}",
                ownerType, ownerId, mcpServerId, toolIds.size());

        // 3. 刷新缓存
        ownerToolBindingContext.refresh();
    }
}
