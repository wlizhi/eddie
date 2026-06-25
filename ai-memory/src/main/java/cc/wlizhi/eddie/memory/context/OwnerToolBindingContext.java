package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.dao.OwnerToolBindingDao;
import cc.wlizhi.eddie.common.dao.ToolDefinitionDao;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owner 工具绑定缓存上下文，全应用生命周期缓存
 * <p>
 * 缓存结构（双层 Map）：
 * <pre>
 *   ownerToolMap: ownerType(ASSISTANT) → ownerId → List<ToolDefinitionEntity>
 *   toolDefMap:   toolId → ToolDefinitionEntity（全量工具定义）
 * </pre>
 * <p>
 * 同时支持 BUILT_IN 和 MCP 两种工具类型，后续添加 MCP 工具无需改此缓存逻辑。
 * 当用户修改工具绑定关系后，调用 {@link #refresh()} 刷新缓存。
 */
@Component
public class OwnerToolBindingContext implements GlobalCache {

    /**
     * ownerType → ownerId → 绑定的工具定义列表
     */
    private volatile Map<String, Map<Long, List<ToolDefinitionEntity>>> ownerToolMap;

    /**
     * toolId → ToolDefinitionEntity（全量）
     */
    private volatile Map<Long, ToolDefinitionEntity> toolDefMap;

    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private OwnerToolBindingDao ownerToolBindingDao;

    @Resource
    private ToolDefinitionDao toolDefinitionDao;

    @PostConstruct
    void init() {
        refresh();
    }

    /**
     * 获取指定 Owner 已绑定的启用的工具定义列表
     *
     * @param ownerType 归属方类型（ASSISTANT / AGENT）
     * @param ownerId   归属方 ID
     * @return 绑定的工具定义列表，无绑定返回空列表
     */
    public List<ToolDefinitionEntity> getBoundTools(String ownerType, Long ownerId) {
        Map<String, Map<Long, List<ToolDefinitionEntity>>> map = ownerToolMap;
        if (map == null) return List.of();

        Map<Long, List<ToolDefinitionEntity>> byOwner = map.get(ownerType);
        if (byOwner == null) return List.of();

        List<ToolDefinitionEntity> tools = byOwner.get(ownerId);
        return tools != null ? tools : List.of();
    }

    /**
     * 根据 toolId 获取工具定义
     */
    public ToolDefinitionEntity getToolDefinition(Long toolId) {
        Map<Long, ToolDefinitionEntity> map = toolDefMap;
        if (map == null) return null;

        ToolDefinitionEntity entity = map.get(toolId);
        if (entity != null) return entity;

        // 缓存未命中则刷新后重试
        refresh();
        return toolDefMap != null ? toolDefMap.get(toolId) : null;
    }

    @Override
    public void refresh() {
        try {
            lock.lock();

            // 1. 全量加载工具定义
            List<ToolDefinitionEntity> allTools = toolDefinitionDao.findAllEnabled();
            Map<Long, ToolDefinitionEntity> defMap = new LinkedHashMap<>();
            for (ToolDefinitionEntity t : allTools) {
                defMap.put(t.getId(), t);
            }
            this.toolDefMap = defMap;

            // 2. 全量加载绑定关系，按 ownerType → ownerId 分组
            List<OwnerToolBindingDao.OwnerToolBindingRow> bindings = ownerToolBindingDao.findAllBindings();
            Map<String, Map<Long, List<ToolDefinitionEntity>>> bindingMap = new LinkedHashMap<>();

            for (OwnerToolBindingDao.OwnerToolBindingRow row : bindings) {
                ToolDefinitionEntity toolDef = defMap.get(row.getToolId());
                if (toolDef == null) continue; // 工具已禁用或不存在则跳过

                bindingMap
                        .computeIfAbsent(row.getOwnerType(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(row.getOwnerId(), k -> new ArrayList<>())
                        .add(toolDef);
            }

            this.ownerToolMap = bindingMap;

        } finally {
            lock.unlock();
        }
    }
}
