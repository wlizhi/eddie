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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@DependsOn("dataSourceScriptDatabaseInitializer") // 指定依赖的Bean名称
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

    public void registerBuiltInTools() {
        if (toolProviders.isEmpty()) {
            log.debug("[工具自动注册] 无内置工具提供者");
            return;
        }

        // 按 MCP Server 名称分组
        Map<String, List<BuiltInToolProvider>> providersByMcp = toolProviders.stream()
                .collect(Collectors.groupingBy(BuiltInToolProvider::getMcpServerName));

        // 查询数据库现有内置工具，按 name 索引
        List<ToolDefinitionEntity> existingList = toolDefinitionDao.findAllBuiltIn();
        Map<String, ToolDefinitionEntity> existingByName = existingList.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));

        int newCount = 0;
        int updateCount = 0;

        for (Map.Entry<String, List<BuiltInToolProvider>> entry : providersByMcp.entrySet()) {
            String mcpName = entry.getKey();
            List<BuiltInToolProvider> providers = entry.getValue();

            // 创建或获取 MCP Server 记录
            Long mcpServerId = resolveMcpServer(mcpName);

            for (BuiltInToolProvider provider : providers) {
                // 使用 Spring AI 的 ToolCallbacks API 发现 @Tool 方法（AOT-safe）
                ToolCallback[] callbacks = org.springframework.ai.support.ToolCallbacks.from(provider);

                for (ToolCallback callback : callbacks) {
                    ToolDefinition def = callback.getToolDefinition();
                    String name = def.name();
                    String description = def.description();
                    String displayName = inferDisplayName(provider.getClass(), name);

                    ToolDefinitionEntity entity = existingByName.get(name);
                    if (entity == null) {
                        // 新增
                        entity = new ToolDefinitionEntity();
                        entity.setToolType(ToolType.BUILT_IN);
                        entity.setName(name);
                        entity.setDisplayName(displayName);
                        entity.setDescription(description);
                        entity.setEnabled(1);
                        entity.setBuiltIn(1);
                        entity.setSortOrder(0);
                        entity.setMcpServerId(mcpServerId);
                        toolDefinitionDao.insert(entity);
                        newCount++;
                        log.info("[工具自动注册] 新增内置工具: {} ({})", name, displayName);
                    } else {
                        // 检查是否有变更（含 mcp_server_id）
                        boolean changed = hasChanged(entity, displayName, description)
                                || !Objects.equals(entity.getMcpServerId(), mcpServerId);
                        if (changed) {
                            entity.setDisplayName(displayName);
                            entity.setDescription(description);
                            entity.setMcpServerId(mcpServerId);
                            toolDefinitionDao.update(entity);
                            updateCount++;
                            log.info("[工具自动注册] 更新内置工具: {} ({})", name, displayName);
                        }
                    }
                    // 无变化则跳过
                }
            }
        }

        if (newCount > 0 || updateCount > 0) {
            log.info("[工具自动注册] 完成: 新增 {} 个, 更新 {} 个, 已注册 {} 个 MCP Server",
                    newCount, updateCount, providersByMcp.size());
        } else {
            log.debug("[工具自动注册] 无变更");
        }
        ownerToolBindingContext.refresh();
    }

    /**
     * 创建或获取 MCP Server 记录
     */
    private Long resolveMcpServer(String mcpName) {
        McpServerEntity existing = mcpServerDao.findByName(mcpName).orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        // 新建 MCP Server
        McpServerEntity entity = new McpServerEntity();
        entity.setName(mcpName);
        entity.setSourceType("BUILT_IN");
        entity.setSourceConfig("{}");
        entity.setTransportType("BUILT_IN");
//        entity.setUrl("http://localhost:" + serverPort + "/mcp/v1/stream");
        entity.setTimeoutSeconds(60);
        entity.setEnabled(1);
        entity.setSortOrder(0);
        mcpServerDao.insert(entity);

        Long newId = mcpServerDao.findLastInsertId();
        log.info("[工具自动注册] 新增内置 MCP Server: {} (id={}, url={})", mcpName, newId, entity.getUrl());
        return newId;
    }

    /**
     * 从工具 name 推断显示名称
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

    /**
     * 判断工具定义是否有实际变更（排除创建/更新时间）
     */
    private boolean hasChanged(ToolDefinitionEntity entity, String displayName, String description) {
        return !Objects.equals(entity.getDisplayName(), displayName)
                || !Objects.equals(entity.getDescription(), description);
    }
}
