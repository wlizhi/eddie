package cc.wlizhi.eddie.tools.service;

import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.enums.McpTransportType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单个 MCP 服务器连接的持有者
 * <p>
 * 职责：
 * <ul>
 *   <li>根据 {@link McpServerEntity} 创建对应传输层的 MCP 客户端</li>
 *   <li>调用 MCP 协议 {@code listTools} 获取远程工具列表并转换为 {@link McpToolCallback}</li>
 *   <li>连接断开后自动重连（可配置间隔和最大重试次数）</li>
 * </ul>
 * <p>
 * 连接状态机：{@code DISCONNECTED → CONNECTED → DISCONNECTED(意外) → RECONNECTING → CONNECTED ...}
 */
@Slf4j
public class McpClientHolder implements AutoCloseable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认重连间隔（秒）
     */
    private static final long DEFAULT_RECONNECT_INTERVAL_SEC = 5;

    /**
     * 连接状态
     */
    @Getter
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;

    /**
     * MCP 同步客户端
     */
    private volatile io.modelcontextprotocol.client.McpSyncClient client;

    /**
     * 缓存解析出的工具回调
     */
    private volatile List<McpToolCallback> callbacks = List.of();

    /**
     * MCP 服务器配置
     */
    private final McpServerEntity server;

    /**
     * 所属 Registry 引用（用于重连成功后通知刷新缓存）
     */
    private final McpClientRegistry registry;

    /**
     * 重连定时任务
     */
    private ScheduledFuture<?> reconnectFuture;

    /**
     * 当前重连尝试次数
     */
    private final AtomicInteger attemptCount = new AtomicInteger(0);

    /**
     * 上次连接失败的错误消息（供上层同步注册时获取）
     */
    private volatile String lastErrorMessage;

    /**
     * 调度器
     */
    private final ScheduledExecutorService scheduler;

    // ==================== 查询状态 ====================

    /**
     * 获取上次连接失败的错误消息
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public enum ConnectionState {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING
    }

    public McpClientHolder(McpServerEntity server, McpClientRegistry registry,
                           ScheduledExecutorService scheduler) {
        this.server = server;
        this.registry = registry;
        this.scheduler = scheduler;
    }

    // ==================== 公共方法 ====================

    /**
     * 建立与 MCP Server 的连接
     *
     * @return true=连接成功, false=连接失败
     */
    public boolean connect() {
        try {
            this.lastErrorMessage = null;
            io.modelcontextprotocol.client.McpSyncClient newClient = createClient(server);

            // 调用 listTools 获取远程工具
            McpSchema.ListToolsResult listResult = newClient.listTools();
            List<McpSchema.Tool> remoteTools = listResult.tools();

            // 转换为 McpToolCallback（传入 mcpServerId 用于生成限定名）
            Long mcpServerId = server.getId();
            List<McpToolCallback> newCallbacks = new ArrayList<>(remoteTools.size());
            for (McpSchema.Tool tool : remoteTools) {
                // 注入断开通知器：传输异常时标记断开并触发重连
                newCallbacks.add(new McpToolCallback(newClient, tool, mcpServerId, () -> {
                    // 只有 CONNECTED 状态才触发，避免重连中重复触发
                    if (state == ConnectionState.CONNECTED) {
                        state = ConnectionState.DISCONNECTED;
                        callbacks = List.of();
                        closeClient();
                        startReconnectTask();
                        log.warn("MCP 连接检测到传输异常，已标记断开并启动重连: {}", server.getName());
                    }
                }));
            }

            this.client = newClient;
            this.callbacks = newCallbacks;
            this.state = ConnectionState.CONNECTED;
            this.attemptCount.set(0);

            log.info("MCP 连接成功: {} (tools={}, transport={})",
                    server.getName(), callbacks.size(), server.getTransportType());
            return true;

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            this.lastErrorMessage = msg;
            log.warn("MCP 连接失败: {} - {}", server.getName(), msg, e);  // 添加 e 打印完整异常栈
            this.state = ConnectionState.DISCONNECTED;
            return false;
        }
    }

    /**
     * 断开并清理客户端
     */
    public void disconnect() {
        stopReconnectTask();
        this.state = ConnectionState.DISCONNECTED;
        closeClient();
        this.callbacks = List.of();
        this.attemptCount.set(0);
        log.info("MCP 已断开: {}", server.getName());
    }

    /**
     * 获取工具回调列表
     */
    public List<McpToolCallback> getToolCallbacks() {
        return callbacks;
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }

    @Override
    public void close() {
        disconnect();
    }

    // ==================== 重连机制 ====================

    /**
     * 启动自动重连任务
     */
    public void startReconnectTask() {
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            return; // 已有重连任务在运行
        }

        long reconnectIntervalSec = server.getReconnectIntervalSec() != null
                && server.getReconnectIntervalSec() > 0
                ? server.getReconnectIntervalSec()
                : DEFAULT_RECONNECT_INTERVAL_SEC;

        // null=默认5次, >0=指定次数（由调用方保证不会传入0）
        int maxAttempts = server.getMaxReconnectAttempts() != null
                ? server.getMaxReconnectAttempts()
                : 5;

        this.state = ConnectionState.RECONNECTING;
        log.info("MCP 启动重连: {} (间隔={}s, 最大尝试={})",
                server.getName(), reconnectIntervalSec, maxAttempts);

        reconnectFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                // 已连接或不再需要重连
                if (state == ConnectionState.CONNECTED || reconnectFuture == null) {
                    return;
                }

                int currentAttempt = attemptCount.incrementAndGet();
                if (currentAttempt > maxAttempts) {
                    log.warn("MCP 重连已达上限 ({}次), 停止重连: {}", maxAttempts, server.getName());
                    stopReconnectTask();
                    return;
                }

                log.info("MCP 重连第 {} 次: {}", currentAttempt, server.getName());
                if (connect()) {
                    // 重连成功 → 通知 Registry
                    registry.onReconnectSuccess(server.getId());
                    stopReconnectTask();
                }
            } catch (Exception e) {
                log.warn("MCP 重连异常: {} - {}", server.getName(), e.getMessage());
            }
        }, reconnectIntervalSec, reconnectIntervalSec, TimeUnit.SECONDS);
    }

    /**
     * 停止重连任务
     */
    public void stopReconnectTask() {
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            reconnectFuture.cancel(false);
        }
        reconnectFuture = null;
    }

    // ==================== 内部方法 ====================

    /**
     * 根据传输类型创建 MCP 客户端
     */
    private io.modelcontextprotocol.client.McpSyncClient createClient(McpServerEntity server) {
        Duration timeout = Duration.ofSeconds(
                server.getTimeoutSeconds() != null ? server.getTimeoutSeconds() : 60);

        McpTransportType transportType = McpTransportType.fromCode(server.getTransportType());
        if (transportType == null) {
            transportType = McpTransportType.STREAMABLE_HTTP;
        }

        return switch (transportType) {
            case STREAMABLE_HTTP -> createStreamableHttpClient(server, timeout);
            case SSE -> createStreamableHttpClient(server, timeout); // SSE 已弃用，统一使用 Streamable HTTP
            case STDIO -> createStdioClient(server, timeout);
        };
    }

    private io.modelcontextprotocol.client.McpSyncClient createStreamableHttpClient(
            McpServerEntity server, Duration timeout) {
        Map<String, String> headers = parseKeyValueMap(server.getHeaders());
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        // 解析 URL，分离基础地址和路径，避免 SDK 默认 endpoint="/mcp" 重复拼接
        URI parsedUri = URI.create(server.getUrl());
        String baseUrl = parsedUri.getScheme() + "://" + parsedUri.getAuthority();
        String endpointPath = parsedUri.getPath();
        if (endpointPath == null || endpointPath.isBlank()) {
            endpointPath = "/mcp";
        }

        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport.builder(baseUrl)
                        .clientBuilder(HttpClient.newBuilder().connectTimeout(timeout))
                        .requestBuilder(requestBuilder)
                        .endpoint(endpointPath)
                        .build();
        return McpClient.sync(transport)
                .requestTimeout(timeout)
                .build();
    }

    private io.modelcontextprotocol.client.McpSyncClient createStdioClient(
            McpServerEntity server, Duration timeout) {
        List<String> argsList = parseJsonArray(server.getArgs());
        Map<String, String> envMap = parseKeyValueMap(server.getEnv());

        ServerParameters params = ServerParameters.builder(server.getCommand())
                .args(argsList)
                .env(envMap)
                .build();

        StdioClientTransport transport = new StdioClientTransport(params, McpJsonDefaults.getMapper());
        return McpClient.sync(transport)
                .requestTimeout(timeout)
                .build();
    }

    private void closeClient() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
                // ignore
            }
            client = null;
        }
    }

    // ==================== JSON 解析工具方法 ====================

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return List.of();
        }
    }

    private Map<String, String> parseJsonObject(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 对象失败: {}", json, e);
            return Map.of();
        }
    }

    /**
     * 解析 key=value 格式的文本（每行一个）。
     * <p>
     * - 空行自动忽略
     * - 每行首尾空白自动 trim
     *
     * @param text 原始存储字符串
     * @return 解析后的 Map
     */
    private Map<String, String> parseKeyValueMap(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String entry = line.trim();
            if (entry.isBlank()) continue;
            int eqIdx = entry.indexOf('=');
            if (eqIdx > 0) {
                String key = entry.substring(0, eqIdx).trim();
                String value = entry.substring(eqIdx + 1).trim();
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }
}
