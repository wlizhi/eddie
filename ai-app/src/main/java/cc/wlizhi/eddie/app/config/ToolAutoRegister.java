package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
 */
@Slf4j
@Component
public class ToolAutoRegister {

    private final List<BuiltInToolProvider> toolProviders;
    private final ToolDefinitionDao toolDefinitionDao;

    public ToolAutoRegister(List<BuiltInToolProvider> toolProviders,
                            ToolDefinitionDao toolDefinitionDao) {
        this.toolProviders = toolProviders;
        this.toolDefinitionDao = toolDefinitionDao;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerBuiltInTools() {
        if (toolProviders.isEmpty()) {
            log.debug("[工具自动注册] 无内置工具提供者");
            return;
        }

        // 查询数据库现有内置工具，按 name 索引
        List<ToolDefinitionEntity> existingList = toolDefinitionDao.findAllBuiltIn();
        Map<String, ToolDefinitionEntity> existingByName = existingList.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));

        int newCount = 0;
        int updateCount = 0;

        for (BuiltInToolProvider provider : toolProviders) {
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
                    entity.setToolType("BUILT_IN");
                    entity.setName(name);
                    entity.setDisplayName(displayName);
                    entity.setDescription(description);
                    entity.setEnabled(1);
                    entity.setBuiltIn(1);
                    entity.setSortOrder(0);
                    toolDefinitionDao.insert(entity);
                    newCount++;
                    log.info("[工具自动注册] 新增内置工具: {} ({})", name, displayName);
                } else if (hasChanged(entity, displayName, description)) {
                    // 有变更 → 更新
                    entity.setDisplayName(displayName);
                    entity.setDescription(description);
                    toolDefinitionDao.update(entity);
                    updateCount++;
                    log.info("[工具自动注册] 更新内置工具: {} ({})", name, displayName);
                }
                // 无变化则跳过
            }
        }

        if (newCount > 0 || updateCount > 0) {
            log.info("[工具自动注册] 完成: 新增 {} 个, 更新 {} 个", newCount, updateCount);
        } else {
            log.debug("[工具自动注册] 无变更");
        }
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
