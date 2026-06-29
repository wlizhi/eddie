package cc.wlizhi.eddie.tools.service;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.enums.McpSourceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP 客户端注册管理中心
 * <p>
 * 全局唯一的 MCP 连接管理器，职责：
 * <ul>
 *   <li>应用启动时加载所有已启用的 MCP Server 并建立连接</li>
 *   <li>提供 {@code register()}/{@code unregister()} 接口供业务层动态控制</li>
 *   <li>供 {@link ToolCallbackResolver} 查询 MCP 工具回调</li>
 *   <li>应用关闭时优雅释放所有连接</li>
 * </ul>
 * <p>
 * 线程安全：使用 {@link ConcurrentHashMap} 保证并发安全，
 * 重连任务使用独立调度线程池。
 */
@Slf4j
@Component
public class McpClientRegistry implements GlobalCache {

    /**
     * mcpServerId → McpClientHolder
     */
    private final ConcurrentHashMap<Long, McpClientHolder> clients = new ConcurrentHashMap<>();

    /**
     * 断开重连专用调度器（守护线程，不阻止 JVM 退出）
     */
    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mcp-reconnect");
                t.setDaemon(true);
                return t;
            });

    @Resource
    private McpServerDao mcpServerDao;

    // ==================== 生命周期 ====================

    /**
     * 应用启动：加载所有已启用的 MCP Server
     */
    @PostConstruct
    public void init() {
        List<McpServerEntity> enabled = mcpServerDao.findAllEnabled();
        if (enabled.isEmpty()) {
            log.debug("MCP 注册中心: 无已启用的 MCP 服务器");
            return;
        }
        log.info("MCP 注册中心: 启动时加载 {} 个 MCP 服务器", enabled.size());
        for (McpServerEntity server : enabled) {
            McpSourceType sourceType = McpSourceType.fromCode(server.getSourceType());
            if (sourceType == McpSourceType.BUILT_IN) {
                continue;
            }
            register(server);
        }
    }

    /**
     * 应用关闭：清理所有连接
     */
    @PreDestroy
    public void destroy() {
        log.info("MCP 注册中心: 关闭中，清理 {} 个连接", clients.size());
        clients.forEach((id, holder) -> {
            try {
                holder.disconnect();
            } catch (Exception ignored) {
                // ignore
            }
        });
        clients.clear();
        reconnectScheduler.shutdownNow();
        try {
            reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            // ignore
        }
        log.info("MCP 注册中心: 已关闭");
    }

    // ==================== 核心操作 ====================

    /**
     * 注册 MCP 服务器：创建客户端连接并获取远程工具
     * <p>
     * 如果已存在相同 ID 的连接，会先关闭旧的再创建新的。
     * 连接成功或失败均会记录日志。
     *
     * @param server MCP 服务器配置
     */
    public void register(McpServerEntity server) {
        if (server == null) {
            log.warn("MCP 注册: server 为 null，跳过");
            return;
        }
        Long id = server.getId();
        if (id == null) {
            log.warn("MCP 注册: server.id 为 null，跳过");
            return;
        }

        // 先关闭旧的连接（如果存在）
        unregister(id);

        // 创建新的连接
        McpClientHolder holder = new McpClientHolder(server, this, reconnectScheduler);
        if (holder.connect()) {
            clients.put(id, holder);
            log.info("MCP 注册成功: {} (id={})", server.getName(), id);
        } else {
            // 连接失败，holder 内部会自动启动重连
            clients.put(id, holder);
            log.warn("MCP 注册失败(将自动重连): {} (id={})", server.getName(), id);
        }
    }

    /**
     * 注销 MCP 服务器：断开连接并释放资源
     *
     * @param mcpServerId MCP 服务器 ID
     */
    public void unregister(Long mcpServerId) {
        if (mcpServerId == null) return;
        McpClientHolder old = clients.remove(mcpServerId);
        if (old != null) {
            old.disconnect();
            log.info("MCP 注销成功: id={}", mcpServerId);
        }
    }

    /**
     * 获取指定 MCP 服务器下的所有 ToolCallback
     * <p>
     * 由 {@link ToolCallbackResolver#resolveMcp} 调用。
     *
     * @param mcpServerId MCP 服务器 ID
     * @return 工具回调列表，连接不存在或未连接时返回空列表
     */
    public List<McpToolCallback> getToolCallbacks(Long mcpServerId) {
        if (mcpServerId == null) return List.of();
        McpClientHolder holder = clients.get(mcpServerId);
        if (holder == null || !holder.isConnected()) {
            return List.of();
        }
        return holder.getToolCallbacks();
    }

    /**
     * 重连成功后的回调
     * <p>
     * 由 {@link McpClientHolder} 在重连成功后调用。
     */
    void onReconnectSuccess(Long mcpServerId) {
        log.info("MCP 重连成功: id={}", mcpServerId);
    }

    /**
     * 刷新：重新加载所有已启用的 MCP Server
     * <p>
     * 实现 {@link GlobalCache#refresh()}，供外部触发全量刷新。
     */
    @Override
    public void refresh() {
        log.info("MCP 注册中心: 全量刷新");
        // 保留当前已连接的 ID 列表
        var oldKeys = clients.keySet();

        // 重新加载数据库中的已启用 MCP
        List<McpServerEntity> enabled = mcpServerDao.findAllEnabled();
        for (McpServerEntity server : enabled) {
            if (!clients.containsKey(server.getId())) {
                register(server);
            }
            oldKeys.remove(server.getId());
        }

        // 剩余的 oldKeys 是数据库中已禁用的，但还在连接中的 → 注销
        for (Long removedId : oldKeys) {
            unregister(removedId);
        }
    }
}
