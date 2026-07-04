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
 * 所有索引（mcpServerToolsIndex、bindingMap）直接存储实体对象引用而非 ID，
 * 避免查询时的二次 Map 查找。实体对象在 heap 中仅存一份，所有索引共享引用。
 * <pre>
 *   mcpServerMap          mcpServerId → McpServerEntity
 *   toolDefMap            toolId → ToolDefinitionEntity（全量，主存储）
 *   toolNameIndex         name → ToolDefinitionEntity（按名称快速查找）
 *   mcpServerToolsIndex   mcpServerId → 有序 List<ToolDefinitionEntity>
 *   sortedMcpServerIds    有序 MCP Server ID 列表
 *   bindingMap            ownerType → ownerId → 有序 List<ToolDefinitionEntity>
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
     * ownerType → ownerId → 有序的工具列表（直接引用 heap 对象）
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
     * 自动过滤：工具全局禁用 → 排除；关联 MCP 服务禁用 → 排除
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
                .filter(t -> t.getEnabled() == 1)                   // 工具全局启用
                .filter(t -> isMcpServerEnabled(t, mcpMap))         // 关联 MCP 服务启用
                .toList();
    }

    /**
     * 判断工具关联的 MCP 服务是否已启用
     */
    private boolean isMcpServerEnabled(ToolDefinitionEntity tool, Map<Long, McpServerEntity> mcpMap) {
        if (tool.getMcpServerId() == null) return true; // BUILT_IN 无关联 MCP
        McpServerEntity mcp = mcpMap.get(tool.getMcpServerId());
        return mcp != null && mcp.getEnabled() == 1;
    }


    // ==================== 查询方法：MCP 服务 ====================

    /**
     * 根据 mcpServerId 获取 MCP 服务
     */
    public McpServerEntity getMcpServer(Long mcpServerId) {
        return mcpServerMap.get(mcpServerId);
    }

    /**
     * 根据服务名称和来源类型从缓存中查找 MCP 服务。<p>
     * 遍历内存中的 {@link #mcpServerMap}，按 name + sourceType 精确匹配。
     *
     * @param name       服务名称（如 "BuiltInShell"）
     * @param sourceType 来源类型（如 "BUILT_IN"），传 null 则忽略此过滤条件
     * @return 匹配的 MCP 服务，不存在返回 null
     */
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

    /**
     * 获取指定 MCP 服务下的工具列表（全量，含禁用）
     */
    public List<ToolDefinitionEntity> getToolsByMcpServerId(Long mcpServerId) {
        List<ToolDefinitionEntity> tools = mcpServerToolsIndex.get(mcpServerId);
        return tools != null ? tools : List.of();
    }

    // ==================== 二层结构查询（前端展示用） ====================

    /**
     * 获取全量 MCP 服务列表及下辖工具（含禁用），按 sort_order 排序
     * <p>
     * 对应场景：管理页面查询所有 MCP + 工具
     */
    public List<McpServerWithTools> getAllMcpServersWithTools() {
        return buildMcpServerWithTools(false, false);
    }

    /**
     * 获取全局已启用的 MCP 服务列表及下辖已启用的工具
     * <p>
     * 对应场景：管理页面查询已启用的 MCP + 工具（全局设置）
     */
    public List<McpServerWithTools> getEnabledMcpServersWithTools() {
        return buildMcpServerWithTools(true, true);
    }

    /**
     * 构建 MCP + 工具二层结构
     *
     * @param filterMcpEnabled  是否只包含已启用的 MCP 服务
     * @param filterToolEnabled 是否只包含已启用的工具
     */
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
                // 过滤启用工具时，无工具则跳过；全量展示时，无工具也显示
                if (filterToolEnabled) continue;
                tools = List.of();
            } else if (filterToolEnabled) {
                tools = tools.stream()
                        .filter(t -> t.getEnabled() == 1)
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
            // 1. 全量加载 MCP 服务
            // 2. 全量加载工具定义
            // 3. 全量加载绑定关系
            // 4. 构建索引
            // 5. 更新缓存
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

        // 2. 全量加载工具定义（所有索引共享同一份 heap 对象引用）
        List<ToolDefinitionEntity> allTools = toolDefinitionDao.findAll();
        Map<Long, ToolDefinitionEntity> defMap = new LinkedHashMap<>(allTools.size());
        Map<String, ToolDefinitionEntity> nameIdx = new LinkedHashMap<>(allTools.size());
        for (ToolDefinitionEntity t : allTools) {
            defMap.put(t.getId(), t);
            // 使用 qualifiedName 作为索引键，不同 MCP 服务的同名工具不冲突
            nameIdx.put(t.getQualifiedName(), t);
            // 建立 MCP → 工具索引（直接引用 heap 对象）
            if (t.getMcpServerId() != null) {
                List<ToolDefinitionEntity> list = mcpToolsIdx.get(t.getMcpServerId());
                if (list != null) list.add(t);
            }
        }

        // 3. 全量加载绑定关系（直接引用 defMap 中的对象）
        List<OwnerToolBindingDao.OwnerToolBindingRow> bindings = ownerToolBindingDao.findAllBindings();
        Map<String, Map<Long, List<ToolDefinitionEntity>>> bMap = new LinkedHashMap<>();
        for (OwnerToolBindingDao.OwnerToolBindingRow row : bindings) {
            ToolDefinitionEntity toolDef = defMap.get(row.getToolId());
            if (toolDef == null) continue;
            bMap.computeIfAbsent(row.getOwnerType(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(row.getOwnerId(), k -> new ArrayList<>())
                    .add(toolDef);
        }

        // 4. 原子替换（不可变 Map，volatile 保证读线程立即可见）
        this.mcpServerMap = Collections.unmodifiableMap(mcpMap);
        this.mcpServerToolsIndex = Collections.unmodifiableMap(mcpToolsIdx);
        this.sortedMcpServerIds = Collections.unmodifiableList(sortedIds);
        this.bindingMap = Collections.unmodifiableMap(bMap);
    }

    // ==================== 内部 VO ====================

    /**
     * MCP 服务 + 下辖工具列表的二层结构
     */
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
