/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.ToolType;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内置工具自动发现注册器。<p>
 * 通过 {@link BuiltInToolProvider} 标记接口注入所有工具 Bean，
 * 使用 Spring AI 的 {@link ToolCallback} API 发现 {@code @Tool} 方法，
 * 与数据库内置工具记录进行全字段比对（排除创建/更新时间），
 * 自动新增或更新变更的字段。
 * <p>
 * 同时根据 {@link BuiltInToolProvider#getMcpServerName()} 自动创建
 * 对应的 MCP Server 记录，工具注册时关联该 MCP Server ID。
 */
@Slf4j
@Component
public class ToolRegisterInitializer {

    private final List<BuiltInToolProvider> toolProviders;
    private final ToolDefinitionDao toolDefinitionDao;
    private final McpServerDao mcpServerDao;
    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;
    @Resource
    private InitScheduler initScheduler;

    public ToolRegisterInitializer(List<BuiltInToolProvider> toolProviders,
                                   ToolDefinitionDao toolDefinitionDao,
                                   McpServerDao mcpServerDao) {
        this.toolProviders = toolProviders;
        this.toolDefinitionDao = toolDefinitionDao;
        this.mcpServerDao = mcpServerDao;
    }

    @PostConstruct
    public void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 10000000, this::registerBuiltInTools);
    }

    // ==================== 主流程 ====================

    public void registerBuiltInTools() {
        if (toolProviders.isEmpty()) {
            log.debug("[工具自动注册] 无内置工具提供者");
            return;
        }

        // 1. 按 MCP Server 名称分组
        Map<String, List<BuiltInToolProvider>> providersByMcp = toolProviders.stream()
                .collect(Collectors.groupingBy(BuiltInToolProvider::getMcpServerName));

        // 2. 同步 MCP Server：确保所有期望的 Server 都存在，返回 name → id 映射
        Map<String, Long> mcpServerNameToId = syncMcpServers(providersByMcp.keySet());

        // 3. 构建期望的工具定义列表（含正确的 mcpServerId）
        List<ToolDefinitionEntity> expectedList = buildExpectedTools(providersByMcp, mcpServerNameToId);

        // 4. 查询数据库实际状态，按 name 索引
        List<ToolDefinitionEntity> dbList = toolDefinitionDao.findAllBuiltIn();
        Map<String, ToolDefinitionEntity> dbByName = dbList.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));

        // 5. 全量比对，分出新增和更新列表
        List<ToolDefinitionEntity> toInsert = new ArrayList<>();
        List<ToolDefinitionEntity> toUpdate = new ArrayList<>();

        for (ToolDefinitionEntity expected : expectedList) {
            ToolDefinitionEntity existing = dbByName.get(expected.getName());
            if (existing == null) {
                toInsert.add(expected);
                log.info("[工具自动注册] 待新增内置工具: {} ({})", expected.getName(), expected.getDisplayName());
            } else if (hasAnyFieldChanged(expected, existing)) {
                expected.setId(existing.getId());
                toUpdate.add(expected);
                log.info("[工具自动注册] 待更新内置工具: {} ({})", expected.getName(), expected.getDisplayName());
            }
        }

        // 6. 批量执行
        if (!toInsert.isEmpty()) {
            toolDefinitionDao.batchInsert(toInsert);
            log.info("[工具自动注册] 批量新增 {} 个内置工具", toInsert.size());
        }
        if (!toUpdate.isEmpty()) {
            for (ToolDefinitionEntity entity : toUpdate) {
                toolDefinitionDao.update(entity);
            }
            log.info("[工具自动注册] 批量更新 {} 个内置工具", toUpdate.size());
        }

        // 7. 修复 Mapping：处理 tool definition 的 mcp_server_id 指向已删除 Server 的情况
        int repaired = repairMapping(mcpServerNameToId);
        if (repaired > 0) {
            log.info("[工具自动注册] 修复 {} 个内置工具的 MCP Server 映射", repaired);
        }

        if (toInsert.isEmpty() && toUpdate.isEmpty() && repaired == 0) {
            log.debug("[工具自动注册] 无变更");
        } else {
            log.info("[工具自动注册] 完成: 新增 {} 个, 更新 {} 个, 修复映射 {} 个, 已注册 {} 个 MCP Server",
                    toInsert.size(), toUpdate.size(), repaired, mcpServerNameToId.size());
        }

        ownerToolBindingContext.refresh();
    }

    // ==================== MCP Server 同步 ====================

    /**
     * 同步内置 MCP Server：遍历期望的 Server 名称列表，确保数据库中都有对应的记录。
     *
     * @param expectedMcpNames 期望的 MCP Server 名称集合
     * @return name → id 映射
     */
    private Map<String, Long> syncMcpServers(Set<String> expectedMcpNames) {
        // 查询数据库中所有 source_type='BUILT_IN' 的 Server，按 name 索引
        List<McpServerEntity> dbServers = mcpServerDao.findAll().stream()
                .filter(s -> "BUILT_IN".equals(s.getSourceType()))
                .toList();
        Map<String, McpServerEntity> dbByName = dbServers.stream()
                .collect(Collectors.toMap(McpServerEntity::getName, s -> s, (a, b) -> a));

        Map<String, Long> result = new HashMap<>();
        for (String name : expectedMcpNames) {
            McpServerEntity existing = dbByName.get(name);
            if (existing != null) {
                result.put(name, existing.getId());
            } else {
                // 缺失 → 新建
                Long newId = createMcpServer(name);
                result.put(name, newId);
                log.info("[工具自动注册] 新增内置 MCP Server: {} (id={})", name, newId);
            }
        }
        return result;
    }

    /**
     * 创建并插入内置 MCP Server 记录，返回自增 ID。
     */
    private Long createMcpServer(String name) {
        McpServerEntity entity = new McpServerEntity();
        entity.setName(name);
        entity.setSourceType("BUILT_IN");
        entity.setSourceConfig("{}");
        entity.setTransportType("BUILT_IN");
        entity.setTimeoutSeconds(60);
        entity.setEnabled(1);
        entity.setSortOrder(0);
        return mcpServerDao.insert(entity);
    }

    // ==================== 期望工具列表构建 ====================

    /**
     * 遍历所有工具提供者，构建期望的 ToolDefinitionEntity 列表。
     */
    private List<ToolDefinitionEntity> buildExpectedTools(
            Map<String, List<BuiltInToolProvider>> providersByMcp,
            Map<String, Long> mcpServerNameToId) {

        List<ToolDefinitionEntity> list = new ArrayList<>();
        for (Map.Entry<String, List<BuiltInToolProvider>> entry : providersByMcp.entrySet()) {
            Long serverId = mcpServerNameToId.get(entry.getKey());
            for (BuiltInToolProvider provider : entry.getValue()) {
                ToolCallback[] callbacks = org.springframework.ai.support.ToolCallbacks.from(provider);
                for (ToolCallback callback : callbacks) {
                    ToolDefinition def = callback.getToolDefinition();
                    list.add(buildEntity(def, provider, serverId));
                }
            }
        }
        return list;
    }

    /**
     * 从 Spring AI ToolDefinition + provider 构建数据库实体。
     */
    private ToolDefinitionEntity buildEntity(ToolDefinition def, BuiltInToolProvider provider, Long mcpServerId) {
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        entity.setToolType(ToolType.BUILT_IN);
        entity.setName(def.name());
        entity.setDisplayName(inferDisplayName(provider.getClass(), def.name()));
        entity.setDescription(def.description());
        entity.setEnabled(1);
        entity.setBuiltIn(1);
        entity.setSortOrder(0);
        entity.setMcpServerId(mcpServerId);
        return entity;
    }

    /**
     * 从工具 name 推断显示名称。
     */
    private String inferDisplayName(Class<?> providerClass, String toolName) {
        String readableClass = providerClass.getSimpleName()
                .replaceAll("(?i)Tools?$", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
        String readableMethod = toolName.replace('_', ' ')
                .replaceAll("([a-z])([A-Z])", "$1 $2");
        return readableClass + " - " + readableMethod;
    }

    // ==================== Mapping 修复 ====================

    /**
     * 修复数据库中 {@code built_in=1} 但 {@code mcp_server_id} 指向已删除 Server 的记录。<p>
     * 场景：用户删除了 MCP Server 记录，但 tool definition 表中的 {@code mcp_server_id}
     * 仍指向已删除的旧 ID。此方法通过工具名称反向查找它应该归属的 MCP Server ID 并修正。
     *
     * @param mcpServerNameToId 有效的 Server name → id 映射
     * @return 修复的记录数
     */
    private int repairMapping(Map<String, Long> mcpServerNameToId) {
        Set<Long> validServerIds = new HashSet<>(mcpServerNameToId.values());
        List<ToolDefinitionEntity> allBuiltIn = toolDefinitionDao.findAllBuiltIn();

        int repaired = 0;
        for (ToolDefinitionEntity toolDef : allBuiltIn) {
            Long currentId = toolDef.getMcpServerId();
            if (currentId == null || validServerIds.contains(currentId)) {
                continue; // 映射正常，跳过
            }

            // mcp_server_id 指向了无效的 Server → 置空
            toolDef.setMcpServerId(null);
            toolDefinitionDao.update(toolDef);
            repaired++;
            log.warn("[Mapping修复] tool '{}' 的 mcp_server_id={} 指向已删除的 Server，已置空",
                    toolDef.getName(), currentId);
        }
        return repaired;
    }

    // ==================== 比对工具 ====================

    /**
     * 全字段比对（排除创建/更新时间），判断是否需要更新。
     */
    private boolean hasAnyFieldChanged(ToolDefinitionEntity expected, ToolDefinitionEntity actual) {
        return !Objects.equals(expected.getDisplayName(), actual.getDisplayName())
                || !Objects.equals(expected.getDescription(), actual.getDescription())
                || !Objects.equals(expected.getToolType(), actual.getToolType())
                || !Objects.equals(expected.getEnabled(), actual.getEnabled())
                || !Objects.equals(expected.getSortOrder(), actual.getSortOrder())
                || !Objects.equals(expected.getMcpServerId(), actual.getMcpServerId());
    }
}
