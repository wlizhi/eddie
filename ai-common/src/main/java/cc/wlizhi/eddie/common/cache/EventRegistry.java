/**
 * @author Eddie
 * {@code @date} 2026-07-01
 */

package cc.wlizhi.eddie.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * 通用事件注册表
 * <p>
 * 用于跨请求的事件通知机制，典型场景：
 * <ul>
 *   <li>用户点击停止回答 → 停止接口写入停止事件 → SSE Flux 检测到后中断流</li>
 *   <li>未来智能体模块的取消/中断场景</li>
 * </ul>
 * <p>
 * 采用惰性过期策略（{@link #get(String)} 时检查是否超时），无需后台定时器。
 * 正常流程由业务方在 {@code doFinally} 中调用 {@link #remove(String)} 清理。
 * <p>
 * <b>注意：</b>聊天模块和智能体模块应各自声明独立的 {@code @Bean} 实例，
 * 使用不同的内部 {@link ConcurrentHashMap}，避免事件互相干扰。
 */
public class EventRegistry {

    private final ConcurrentHashMap<String, EventEntry<?>> registry = new ConcurrentHashMap<>();

    /**
     * 待完成的 Future 映射——用于 {@link #waitFor(String)} 阻塞等待机制
     * <p>
     * 与 registry 独立存储，互不干扰。当 register() 写入 key 时，
     * 同时 complete 对应的 Future，唤醒等待者。
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingFutures = new ConcurrentHashMap<>();

    /**
     * 默认 TTL：1 分钟（惰性过期）
     */
    private static final long DEFAULT_TTL_MS = 60_000;

    /**
     * 注册事件
     *
     * @param key  事件键，格式为 "{eventType}:{bizId}"，如 "STOP:userMsg_123"
     * @param data 事件数据
     * @param <T>  数据类型
     */
    public <T> void register(String key, T data) {
        registry.put(key, new EventEntry<>(data, System.currentTimeMillis()));
        // 通知正在 waitFor 的等待者（无等待者时无操作）
        CompletableFuture<Object> future = pendingFutures.get(key);
        if (future != null) {
            future.complete(data);
        }
    }

    public <T> void register(String type, String bizId, T data) {
        String k = key(type, bizId);
        registry.put(k, new EventEntry<>(data, System.currentTimeMillis()));
        // 通知正在 waitFor 的等待者（无等待者时无操作）
        CompletableFuture<Object> future = pendingFutures.get(k);
        if (future != null) {
            future.complete(data);
        }
    }

    /**
     * 获取事件数据（惰性过期：如果条目已超 TTL，自动移除并返回 null）
     *
     * @param key 事件键
     * @param <T> 数据类型
     * @return 事件数据，如果不存在或已过期返回 null
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(String key) {
        EventEntry<?> entry = registry.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.createdAt > DEFAULT_TTL_MS) {
            registry.remove(key);
            return null;
        }
        return (T) entry.data;
    }

    /**
     * 检查事件是否存在且未过期
     *
     * @param key 事件键
     * @return true 如果存在且未过期
     */
    public boolean contains(String key) {
        EventEntry<?> entry = registry.get(key);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() - entry.createdAt > DEFAULT_TTL_MS) {
            registry.remove(key);
            return false;
        }
        return true;
    }

    /**
     * 移除事件
     *
     * @param key 事件键
     */
    public void remove(String key) {
        registry.remove(key);
        CompletableFuture<Object> future = pendingFutures.get(key);
        if (future != null) {
            future.complete(null);
        }
        pendingFutures.remove(key);
    }

    /**
     * 阻塞等待事件到达，支持同时监听多个取消事件（如停止信号）
     * <p>
     * 用于工具审批场景——阻塞等待用户批准/拒绝，同时监听停止事件。
     * 当 {@code mainKey} 或任一 {@code cancelKeys} 被 {@link #register(String, Object)} 触发时解除阻塞。
     * <ul>
     *   <li>主事件到达 → 立即返回数据</li>
     *   <li>取消事件到达 → 返回 {@code null}</li>
     *   <li>虚拟线程被 {@link Thread#interrupt()}（断连）→ 返回 {@code null}</li>
     * </ul>
     *
     * @param mainKey    主事件键，返回值关联此 key
     * @param cancelKeys 可选的取消事件 keys，任一到达视为取消，返回 null
     * @param <T>        数据类型
     * @return 主事件数据；取消/中断返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T waitFor(String mainKey, String... cancelKeys) {
        // 先检查主事件是否已存在
        T existing = get(mainKey);
        if (existing != null) {
            registry.remove(mainKey);
            return existing;
        }

        CompletableFuture<Object> mainFuture = new CompletableFuture<>();
        pendingFutures.put(mainKey, mainFuture);

        List<CompletableFuture<Object>> allFutures = new ArrayList<>();
        allFutures.add(mainFuture);
        for (String cancelKey : cancelKeys) {
            CompletableFuture<Object> cf = new CompletableFuture<>();
            pendingFutures.put(cancelKey, cf);
            allFutures.add(cf);
        }

        try {
            if (allFutures.size() == 1) {
                // 无取消 key，等同于原 waitFor 行为
                return (T) mainFuture.get();
            }
            // 等待主事件或任一取消事件
            CompletableFuture.anyOf(allFutures.toArray(new CompletableFuture[0])).get();
            if (!mainFuture.isDone()) {
                return null; // 取消事件触发
            }
            return (T) mainFuture.getNow(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        } finally {
            pendingFutures.remove(mainKey);
            for (String cancelKey : cancelKeys) {
                pendingFutures.remove(cancelKey);
            }
            registry.remove(mainKey);
        }
    }

    /**
     * 构建事件键
     *
     * @param type  事件类型前缀
     * @param bizId 业务 ID
     * @return 事件键字符串
     */
    public static String key(String type, String bizId) {
        return type + ":" + bizId;
    }

    /**
     * 事件条目包装
     *
     * @param <T> 数据类型
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventEntry<T> {
        private T data;
        private long createdAt;

    }
}
