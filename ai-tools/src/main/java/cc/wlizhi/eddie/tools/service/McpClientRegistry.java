package cc.wlizhi.eddie.tools.service;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.McpServerDao;
import cc.wlizhi.eddie.common.dto.McpConnectInfo;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.enums.McpSourceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    @Resource
    private InitScheduler initScheduler;

    // ==================== 重连回调 ====================

    /**
     * 重连成功回调接口
     * <p>
     * 用于解耦模块依赖：{@link McpClientRegistry} 在 ai-tools 模块，
     * 业务模块（如 ai-settings）通过此回调注册重连同步逻辑。
     */
    @FunctionalInterface
    public interface ReconnectCallback {
        void onReconnect(Long mcpServerId, List<McpToolCallback> callbacks);
    }

    private final List<ReconnectCallback> reconnectCallbacks = new CopyOnWriteArrayList<>();

    /**
     * 注册重连成功回调
     */
    public void addReconnectCallback(ReconnectCallback callback) {
        if (callback != null) {
            reconnectCallbacks.add(callback);
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 应用启动：加载所有已启用的 MCP Server
     */
    @PostConstruct
    public void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 100, this::doInit);
    }

    private void doInit() {
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
            // 连接失败，保留 holder
            clients.put(id, holder);
            // maxReconnectAttempts: null=默认5次, 0=不重连, >0=重连N次
            Integer maxAttempts = server.getMaxReconnectAttempts();
            if (maxAttempts == null || maxAttempts > 0) {
                holder.startReconnectTask();
                log.warn("MCP 注册失败(将自动重连): {} (id={})", server.getName(), id);
            } else {
                log.warn("MCP 注册失败(不重连): {} (id={})", server.getName(), id);
            }
        }
    }

    /**
     * 同步注册 MCP 服务器：等待连接结果并返回工具列表
     * <p>
     * 与 {@link #register(McpServerEntity)} 的区别：
     * <ul>
     *   <li>同步等待连接完成（连接失败时不会启动后台重连）</li>
     *   <li>返回 {@link McpConnectInfo} 包含连接成功/失败状态、错误消息、远程工具列表</li>
     * </ul>
     * 适用于：用户手动启用 MCP、手动触发工具同步等需要实时反馈的场景。
     *
     * @param server MCP 服务器配置
     * @return 连接结果
     */
    public McpConnectInfo registerSync(McpServerEntity server) {
        if (server == null || server.getId() == null) {
            return McpConnectInfo.failure("MCP 服务器配置无效");
        }

        // 先关闭旧的连接（如果存在）
        unregister(server.getId());

        // 创建新连接（同步，不启动重连调度器）
        McpClientHolder holder = new McpClientHolder(server, this, reconnectScheduler);
        if (holder.connect()) {
            clients.put(server.getId(), holder);
            log.info("MCP 同步注册成功: {} (id={}, tools={})",
                    server.getName(), server.getId(), holder.getToolCallbacks().size());

            // 提取工具信息（使用原始工具名，非限定名）
            List<McpConnectInfo.ToolInfo> toolInfos = new ArrayList<>();
            for (McpToolCallback callback : holder.getToolCallbacks()) {
                var def = callback.getToolDefinition();
                toolInfos.add(new McpConnectInfo.ToolInfo(
                        callback.getOriginalToolName(), def.description(), def.inputSchema()));
            }
            return McpConnectInfo.success(toolInfos);
        } else {
            // 连接失败，不保留 holder（不启动重连）
            String errorMsg = holder.getLastErrorMessage();
            log.warn("MCP 同步注册失败: {} (id={}) - {}", server.getName(), server.getId(), errorMsg);
            return McpConnectInfo.failure(errorMsg != null ? errorMsg : "连接失败，未知错误");
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
     * 获取指定 MCP 服务器当前连接状态
     *
     * @return CONNECTED / DISCONNECTED / RECONNECTING，未注册时返回 DISCONNECTED
     */
    public McpClientHolder.ConnectionState getConnectionState(Long mcpServerId) {
        if (mcpServerId == null) return McpClientHolder.ConnectionState.DISCONNECTED;
        McpClientHolder holder = clients.get(mcpServerId);
        if (holder == null) return McpClientHolder.ConnectionState.DISCONNECTED;
        return holder.getState();
    }

    /**
     * 重连成功后的回调
     * <p>
     * 由 {@link McpClientHolder} 在重连成功后调用。
     */
    void onReconnectSuccess(Long mcpServerId) {
        log.info("MCP 重连成功: id={}", mcpServerId);
        // 遍历回调，通知业务层同步工具等
        McpClientHolder holder = clients.get(mcpServerId);
        if (holder != null && !reconnectCallbacks.isEmpty()) {
            List<McpToolCallback> callbacks = holder.getToolCallbacks();
            for (ReconnectCallback cb : reconnectCallbacks) {
                try {
                    cb.onReconnect(mcpServerId, callbacks);
                } catch (Exception e) {
                    log.error("MCP 重连回调执行失败: id={}", mcpServerId, e);
                }
            }
        }
    }

    /**
     * 测试 MCP 服务器连接：仅验证连通性，不注册、不写入缓存
     * <p>
     * 与 {@link #registerSync(McpServerEntity)} 的区别：
     * <ul>
     *   <li>不校验 server.id（临时连接测试，可能尚未保存到 DB）</li>
     *   <li>连接成功后主动断开清理（测试用，不保持长连接）</li>
     *   <li>不放入 clients 管理</li>
     *   <li>不调用 unregister</li>
     * </ul>
     *
     * @param server MCP 服务器配置（可能不含 id）
     * @return 连接结果
     */
    public McpConnectInfo testConnection(McpServerEntity server) {
        if (server == null) {
            return McpConnectInfo.failure("MCP 服务器配置无效");
        }

        // 创建临时连接（同步，不启动重连调度器）
        McpClientHolder holder = new McpClientHolder(server, this, reconnectScheduler);
        if (holder.connect()) {
            log.info("MCP 连接测试成功: name={}, tools={}",
                    server.getName(), holder.getToolCallbacks().size());

            // 提取工具信息（使用原始工具名，非限定名）
            List<McpConnectInfo.ToolInfo> toolInfos = new ArrayList<>();
            for (McpToolCallback callback : holder.getToolCallbacks()) {
                var def = callback.getToolDefinition();
                toolInfos.add(new McpConnectInfo.ToolInfo(
                        callback.getOriginalToolName(), def.description(), def.inputSchema()));
            }

            // 测试完毕，主动断开清理
            holder.disconnect();
            return McpConnectInfo.success(toolInfos);
        } else {
            String errorMsg = holder.getLastErrorMessage();
            log.warn("MCP 连接测试失败: name={} - {}", server.getName(), errorMsg);
            return McpConnectInfo.failure(errorMsg != null ? errorMsg : "连接失败，未知错误");
        }
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
