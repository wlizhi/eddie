/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.McpSourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 全局工具相关缓存上下文，全应用生命周期缓存
 * <p>
 * 缓存三块数据及其索引：
 * <ul>
 *   <li>{@link McpServerEntity} — MCP 服务定义</li>
 *   <li>{@link ToolDefinitionEntity} — 全量工具定义（含禁用）</li>
 *   <li>绑定关系（owner → 工具引用列表）</li>
 * </ul>
 * <p>
 * <b>关键语义：</b><br>
 * bindingMap 中存储的 ToolDefinitionEntity 是<b>克隆副本</b>，其 {@code enabled} 字段
 * 反映的是 <b>owner 级别绑定状态</b>（ai_owner_tool_binding.enabled），而非全局工具定义
 * 状态（ai_tool_definition.enabled）。调用方通过 {@link #getBoundTools} 获取工具列表后，
 * {@code t.getEnabled()} 即可得到该 owner 的真实启用/待审批状态。
 * <pre>
 *   mcpServerMap          mcpServerId → McpServerEntity
 *   mcpServerToolsIndex   mcpServerId → 有序 List<ToolDefinitionEntity>
 *   sortedMcpServerIds    有序 MCP Server ID 列表
 *   bindingMap            ownerType → ownerId → 有序 List<ToolDefinitionEntity>（克隆副本）
 * </pre>
 * <p>
 * 查询 100% 走内存，刷新时全量重建后原子替换（copy-on-write），无需加锁。
 * 数据量 MCP < 10，工具 < 100，绑定 < 500，全量加载开销可忽略。
 */
@Slf4j
@Component
public class OwnerToolBindingContext implements GlobalCache {
    private final ReentrantLock lock = new ReentrantLock();

    // ==================== 缓存数据（不可变 Map，volatile 保证可见性） ====================

    /**
     * mcpServerId → McpServerEntity（全量，含禁用）
     */
    private volatile Map<Long, McpServerEntity> mcpServerMap = Map.of();

    /**
     * mcpServerId → 有序的工具列表（全量，含禁用，直接引用 heap 对象）
     */
    private volatile Map<Long, List<ToolDefinitionEntity>> mcpServerToolsIndex = Map.of();

    /**
     * 按 sort_order 排序的 MCP Server ID 列表（全量）
     */
    private volatile List<Long> sortedMcpServerIds = List.of();

    /**
     * ownerType → ownerId → 有序的工具列表（克隆副本，enabled 反映 owner 级别绑定状态）
     */
    private volatile Map<String, Map<Long, List<ToolDefinitionEntity>>> bindingMap = Map.of();

    // ==================== DAOs ====================

    @Resource
    private McpServerDao mcpServerDao;

    @Resource
    private ToolDefinitionDao toolDefinitionDao;

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 查询方法：绑定关系 ====================

    /**
     * 获取指定 Owner 已绑定的启用的工具定义列表（联动 MCP 服务启用状态）
     * <p>
     * 返回的 ToolDefinitionEntity 中 {@code enabled} 反映的是 <b>owner 级别绑定状态</b>
     * （非全局工具定义状态）：
     * <ul>
     *   <li>1 → 已启用</li>
     *   <li>2 → 待审批</li>
     * </ul>
     * 自动过滤：owner 级别绑定禁用 → 排除；关联 MCP 服务禁用 → 排除
     *
     * @param ownerType 归属方类型（ASSISTANT / AGENT）
     * @param ownerId   归属方 ID
     * @return 绑定的工具定义列表，无绑定返回空列表
     */
    public List<ToolDefinitionEntity> getBoundTools(String ownerType, Long ownerId) {
        Map<Long, List<ToolDefinitionEntity>> byOwner = bindingMap.get(ownerType);
        if (byOwner == null) return List.of();

        List<ToolDefinitionEntity> tools = byOwner.get(ownerId);
        if (tools == null) return List.of();

        Map<Long, McpServerEntity> mcpMap = this.mcpServerMap;

        return tools.stream()
                .filter(t -> isBindingActive(t))                    // owner 级别绑定启用或待审批
                .filter(t -> isMcpServerEnabled(t, mcpMap))         // 关联 MCP 服务启用
                .toList();
    }

    /**
     * 判断 owner 级别绑定是否激活（启用 或 待审批）
     * <p>
     * 注意：此处的 enabled 已由 {@link #doRefresh()} 覆写为 binding 级别值，
     * 非全局 ToolDefinition 的 enabled。
     */
    private boolean isBindingActive(ToolDefinitionEntity tool) {
        Integer enabled = tool.getEnabled();
        return enabled != null && (enabled == 1 || enabled == 2);
    }

    /**
     * 判断工具关联的 MCP 服务是否已启用
     */
    private boolean isMcpServerEnabled(ToolDefinitionEntity tool, Map<Long, McpServerEntity> mcpMap) {
        if (tool.getMcpServerId() == null) return true;
        McpServerEntity mcp = mcpMap.get(tool.getMcpServerId());
        return mcp != null && mcp.getEnabled() == 1;
    }


    // ==================== 查询方法：MCP 服务 ====================

    public McpServerEntity getMcpServer(Long mcpServerId) {
        return mcpServerMap.get(mcpServerId);
    }

    public McpServerEntity getBuiltInMcpServerByName(String name) {
        if (name == null) return null;
        String sourceType = McpSourceType.BUILT_IN.name();
        for (McpServerEntity mcp : mcpServerMap.values()) {
            if (name.equals(mcp.getName()) && sourceType.equals(mcp.getSourceType())) {
                return mcp;
            }
        }
        return null;
    }

    public List<ToolDefinitionEntity> getToolsByMcpServerId(Long mcpServerId) {
        List<ToolDefinitionEntity> tools = mcpServerToolsIndex.get(mcpServerId);
        return tools != null ? tools : List.of();
    }

    // ==================== 二层结构查询（前端展示用） ====================

    public List<McpServerWithTools> getAllMcpServersWithTools() {
        return buildMcpServerWithTools(false, false);
    }

    public List<McpServerWithTools> getEnabledMcpServersWithTools() {
        return buildMcpServerWithTools(true, true);
    }

    private List<McpServerWithTools> buildMcpServerWithTools(boolean filterMcpEnabled, boolean filterToolEnabled) {
        Map<Long, McpServerEntity> mcpMap = this.mcpServerMap;
        Map<Long, List<ToolDefinitionEntity>> mcpToolsIdx = this.mcpServerToolsIndex;
        List<Long> sortedIds = this.sortedMcpServerIds;

        List<McpServerWithTools> result = new ArrayList<>(sortedIds.size());
        for (Long mcpServerId : sortedIds) {
            McpServerEntity mcp = mcpMap.get(mcpServerId);
            if (mcp == null) continue;
            if (filterMcpEnabled && mcp.getEnabled() != 1) continue;

            List<ToolDefinitionEntity> tools = mcpToolsIdx.get(mcpServerId);
            if (tools == null || tools.isEmpty()) {
                if (filterToolEnabled) continue;
                tools = List.of();
            } else if (filterToolEnabled) {
                tools = tools.stream()
                        .filter(t -> t.getEnabled() != null && t.getEnabled() == 1)
                        .toList();
                if (tools.isEmpty()) continue;
            }

            result.add(new McpServerWithTools(mcp, tools));
        }
        return result;
    }

    // ==================== 缓存刷新 ====================

    @Override
    public void refresh() {
        lock.lock();
        try {
            log.info("开始刷新内置工具缓存");
            doRefresh();
            log.info("刷新内置工具缓存完成");
            log.debug("缓存数据: {}", objectMapper.writeValueAsString(getAllMcpServersWithTools()));
        } catch (Exception e) {
            log.warn("写入缓存数据日志失败（非关键错误）: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void doRefresh() {
        // 1. 全量加载 MCP 服务
        List<McpServerEntity> allMcpServers = mcpServerDao.findAll();
        Map<Long, McpServerEntity> mcpMap = new LinkedHashMap<>(allMcpServers.size());
        Map<Long, List<ToolDefinitionEntity>> mcpToolsIdx = new LinkedHashMap<>(allMcpServers.size());
        List<Long> sortedIds = new ArrayList<>(allMcpServers.size());
        for (McpServerEntity m : allMcpServers) {
            mcpMap.put(m.getId(), m);
            mcpToolsIdx.put(m.getId(), new ArrayList<>());
            sortedIds.add(m.getId());
        }

        // 2. 全量加载工具定义（mcpServerToolsIndex 共享全局 heap 对象引用）
        List<ToolDefinitionEntity> allTools = toolDefinitionDao.findAll();
        Map<Long, ToolDefinitionEntity> defMap = new LinkedHashMap<>(allTools.size());
        for (ToolDefinitionEntity t : allTools) {
            defMap.put(t.getId(), t);
            if (t.getMcpServerId() != null) {
                List<ToolDefinitionEntity> list = mcpToolsIdx.get(t.getMcpServerId());
                if (list != null) list.add(t);
            }
        }

        // 3. 全量加载绑定关系
        //    克隆 ToolDefinitionEntity 并将 enabled 覆写为 binding 级别值，
        //    使得调用方通过 t.getEnabled() 即可获取 owner 级别启用/待审批状态
        List<OwnerToolBindingDao.OwnerToolBindingRow> bindings = ownerToolBindingDao.findAllBindings();
        Map<String, Map<Long, List<ToolDefinitionEntity>>> bMap = new LinkedHashMap<>();
        for (OwnerToolBindingDao.OwnerToolBindingRow row : bindings) {
            ToolDefinitionEntity proto = defMap.get(row.getToolId());
            if (proto == null) continue;

            ToolDefinitionEntity cloned = cloneWithBindingEnabled(proto, row.getEnabled());
            bMap.computeIfAbsent(row.getOwnerType(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(row.getOwnerId(), k -> new ArrayList<>())
                    .add(cloned);
        }

        // 4. 原子替换（不可变 Map，volatile 保证读线程立即可见）
        this.mcpServerMap = Collections.unmodifiableMap(mcpMap);
        this.mcpServerToolsIndex = Collections.unmodifiableMap(mcpToolsIdx);
        this.sortedMcpServerIds = Collections.unmodifiableList(sortedIds);
        this.bindingMap = Collections.unmodifiableMap(bMap);
    }

    /**
     * 克隆 ToolDefinitionEntity 并将 enabled 覆写为 binding 级别状态值
     * <p>
     * bindingMap 中不共享全局 defMap 的对象引用，而是持有克隆副本，
     * 使得 {@code enabled} 反映 owner 级别绑定状态（而非全局工具定义状态）。
     */
    private static ToolDefinitionEntity cloneWithBindingEnabled(ToolDefinitionEntity proto, Integer bindingEnabled) {
        ToolDefinitionEntity cloned = new ToolDefinitionEntity();
        cloned.setId(proto.getId());
        cloned.setToolType(proto.getToolType());
        cloned.setName(proto.getName());
        cloned.setDisplayName(proto.getDisplayName());
        cloned.setDescription(proto.getDescription());
        cloned.setEnabled(bindingEnabled);          // 覆写为 binding 级别值
        cloned.setBuiltIn(proto.getBuiltIn());
        cloned.setMcpServerId(proto.getMcpServerId());
        cloned.setSortOrder(proto.getSortOrder());
        cloned.setCreatedAt(proto.getCreatedAt());
        cloned.setUpdatedAt(proto.getUpdatedAt());
        return cloned;
    }

    // ==================== 内部 VO ====================

    @lombok.Getter
    public static class McpServerWithTools {
        private final McpServerEntity mcpServer;
        private final List<ToolDefinitionEntity> tools;

        public McpServerWithTools(McpServerEntity mcpServer, List<ToolDefinitionEntity> tools) {
            this.mcpServer = mcpServer;
            this.tools = tools;
        }
    }
}
