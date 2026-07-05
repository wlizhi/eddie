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

import java.util.concurrent.ConcurrentHashMap;

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
    }

    public <T> void register(String type, String bizId, T data) {
        registry.put(key(type, bizId), new EventEntry<>(data, System.currentTimeMillis()));
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
