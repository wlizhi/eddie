package cc.wlizhi.eddie.tools.service;

import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.ToolType;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具回调解析器
 * <p>
 * 根据助手已绑定的工具定义，将数据库中的工具记录解析为 Spring AI 可执行的 {@link ToolCallback} 数组。
 * 解析策略按 {@link ToolType} 分支：
 * <ul>
 *   <li>{@link ToolType#BUILT_IN} — 从内存中的 {@link BuiltInToolProvider} Bean 通过 {@code ToolCallbacks.from()} 获取</li>
 *   <li>{@link ToolType#MCP} — 从 {@link McpClientRegistry} 获取已连接的远程 MCP 工具</li>
 * </ul>
 * <p>
 * 缓存策略：BUILT_IN 的 ToolCallback 在 Bean 初始化时构建一次，后续仅做名称匹配查找。
 * MCP 的 ToolCallback 由 {@link McpClientRegistry} 管理，连接建立时自动注册，断开时自动移除。
 */
@Service
public class ToolCallbackResolver {

    private static final Logger log = LoggerFactory.getLogger(ToolCallbackResolver.class);

    @Resource
    private List<BuiltInToolProvider> toolProviders;

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private McpClientRegistry mcpClientRegistry;

    /**
     * BUILT_IN 工具名 → ToolCallback 的缓存（应用生命周期内不变）
     */
    private volatile Map<String, ToolCallback> builtInCallbackMap;

    /**
     * 解析指定 Owner 可用的 ToolCallback 列表
     *
     * @param ownerType         归属方类型（ASSISTANT / AGENT）
     * @param ownerId           归属方 ID
     * @param toolSelectionMode 工具选择模式（auto / manual / none）
     * @param toolNames         手动模式下指定的工具名称列表（为空表示不使用工具，仅 manual 模式有效）
     * @return ToolCallback 数组，无可用的工具时返回空数组
     */
    public ToolCallback[] resolve(String ownerType, Long ownerId, String toolSelectionMode, List<String> toolNames) {
        // 不使用工具
        if (toolSelectionMode == null || "none".equals(toolSelectionMode)) {
            return new ToolCallback[0];
        }

        // 从缓存中获取该 owner 绑定的工具定义
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(ownerType, ownerId);
        if (boundTools.isEmpty()) {
            return new ToolCallback[0];
        }

        // manual 模式：只加载指定名称的工具
        if ("manual".equals(toolSelectionMode)) {
            if (toolNames == null || toolNames.isEmpty()) {
                return new ToolCallback[0];
            }
            // 只保留在 toolNames 中的工具
            boundTools = boundTools.stream()
                    .filter(t -> toolNames.contains(t.getName()))
                    .toList();
            if (boundTools.isEmpty()) {
                return new ToolCallback[0];
            }
        }

        // auto 模式：加载所有绑定的工具

        // 按 tool_type 分组解析
        List<ToolCallback> result = new ArrayList<>(boundTools.size());
        for (ToolDefinitionEntity tool : boundTools) {
            ToolType toolType = tool.getToolType();
            try {
                if (toolType == ToolType.BUILT_IN) {
                    resolveBuiltIn(tool).ifPresent(result::add);
                } else if (toolType == ToolType.MCP) {
                    resolveMcp(tool).ifPresent(result::add);
                } else {
                    log.warn("[ToolCallbackResolver] 未知工具类型: {} (name={})", toolType, tool.getName());
                }
            } catch (Exception e) {
                log.error("[ToolCallbackResolver] 解析工具失败: {} (type={})", tool.getName(), toolType, e);
            }
        }

        return result.toArray(new ToolCallback[0]);
    }

    /**
     * 解析内置工具
     */
    private Optional<ToolCallback> resolveBuiltIn(ToolDefinitionEntity toolDef) {
        Map<String, ToolCallback> map = getBuiltInCallbackMap();
        ToolCallback callback = map.get(toolDef.getName());
        if (callback == null) {
            log.warn("[ToolCallbackResolver] 未找到内置工具: {} (可能已被删除或 Bean 未注册)", toolDef.getName());
        }
        return Optional.ofNullable(callback);
    }

    /**
     * 解析 MCP 工具
     * <p>
     * 从 {@link McpClientRegistry} 获取已连接的 MCP 客户端工具回调，
     * 按工具名称匹配返回。
     */
    private Optional<ToolCallback> resolveMcp(ToolDefinitionEntity toolDef) {
        Long mcpServerId = toolDef.getMcpServerId();
        if (mcpServerId == null) {
            log.warn("[ToolCallbackResolver] MCP 工具缺少 mcpServerId: {}", toolDef.getName());
            return Optional.empty();
        }

        List<McpToolCallback> callbacks = mcpClientRegistry.getToolCallbacks(mcpServerId);
        if (callbacks.isEmpty()) {
            log.warn("[ToolCallbackResolver] MCP 客户端未连接或无可用工具: mcpServerId={}, tool={}",
                    mcpServerId, toolDef.getName());
            return Optional.empty();
        }

        // 按工具名称匹配
        for (McpToolCallback callback : callbacks) {
            if (callback.getToolDefinition().name().equals(toolDef.getName())) {
                return Optional.of(callback);
            }
        }

        log.warn("[ToolCallbackResolver] MCP 工具未找到: {} (mcpServerId={}), 可用工具: {}",
                toolDef.getName(), mcpServerId,
                callbacks.stream().map(c -> c.getToolDefinition().name()).toList());
        return Optional.empty();
    }

    /**
     * 获取 BUILT_IN 工具名 → ToolCallback 的缓存 Map
     * <p>
     * 延迟初始化，首次调用时遍历所有 {@link BuiltInToolProvider} Bean，
     * 通过 Spring AI 的 {@code ToolCallbacks.from()} 提取 {@link ToolCallback}。
     */
    private Map<String, ToolCallback> getBuiltInCallbackMap() {
        Map<String, ToolCallback> map = this.builtInCallbackMap;
        if (map != null) return map;

        synchronized (this) {
            map = this.builtInCallbackMap;
            if (map != null) return map;

            Map<String, ToolCallback> newMap = new ConcurrentHashMap<>();
            for (BuiltInToolProvider provider : toolProviders) {
                org.springframework.ai.tool.ToolCallback[] callbacks =
                        org.springframework.ai.support.ToolCallbacks.from(provider);
                for (ToolCallback callback : callbacks) {
                    String name = callback.getToolDefinition().name();
                    if (newMap.put(name, callback) != null) {
                        log.warn("[ToolCallbackResolver] 工具名称冲突: {} (来自 {})", name, provider.getClass().getName());
                    } else {
                        log.debug("[ToolCallbackResolver] 注册内置工具: {} (来自 {})", name, provider.getClass().getSimpleName());
                    }
                }
            }
            this.builtInCallbackMap = newMap;
            return newMap;
        }
    }
}
