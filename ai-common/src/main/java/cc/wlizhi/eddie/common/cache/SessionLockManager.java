/**
 * @author Eddie
 * {@code @date} 2026-07-01
 */

package cc.wlizhi.eddie.common.cache;

import cc.wlizhi.eddie.common.event.SessionDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session 级别的互斥锁管理器
 * <p>
 * 使用 {@link System#nanoTime()} 作为锁 token，通过 {@link ConcurrentHashMap#putIfAbsent} 原子抢锁、
 * {@link ConcurrentHashMap#remove(Object, Object)} 原子比对+释放、{@link ConcurrentHashMap#replace(Object, Object, Object)}
 * 原子覆盖过期锁，确保多线程环境下的绝对安全性，无线程亲缘性问题。
 * <p>
 * 锁 token 本身携带时间属性，无需额外的过期时间戳字段。
 * 当 {@link #tryLock} 发现锁持有者超时（{@link #LOCK_TIMEOUT_NS}），通过 {@code replace(key, oldValue, newValue)}
 * 原子覆盖抢锁，不经过中间空值状态。
 * <p>
 * 锁缓存的生命周期绑定到会话生命周期：会话删除时通过监听 {@link SessionDeletedEvent} 清理对应条目。
 * 单条目内存开销约 80 B，即使每秒创建 1 个会话并运行 24 小时，总泄漏也约 7 MB，可以忽略。
 */
@Component
public class SessionLockManager {

    private static final Logger log = LoggerFactory.getLogger(SessionLockManager.class);

    /**
     * 锁超时阈值（纳秒）：5 分钟
     */
    private static final long LOCK_TIMEOUT_NS = Duration.ofMinutes(5).toNanos();

    private final ConcurrentHashMap<Long, Long> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 尝试获取会话锁
     *
     * @param sessionId 会话 ID
     * @return 锁 token（{@link System#nanoTime()}），0 表示获取失败
     */
    public long tryLock(Long sessionId) {
        long now = System.nanoTime();
        Long existing = sessionLocks.putIfAbsent(sessionId, now);
        if (existing == null) {
            return now;
        }

        // 锁被占用，检查是否超时
        if (now - existing > LOCK_TIMEOUT_NS) {
            long newToken = System.nanoTime();
            // 原子覆盖：仅当值没变时才替换，不经过空值状态
            if (sessionLocks.replace(sessionId, existing, newToken)) {
                log.warn("会话 {} 的锁已超时重新获取", sessionId);
                return newToken;
            }
            // replace 失败 → 值已被其他线程更新，新值肯定没过期
        }

        log.warn("会话 {} 正在处理中，拒绝并发请求", sessionId);
        return 0L;
    }

    /**
     * 释放会话锁
     * <p>
     * 原子比对+释放：仅当锁的当前值等于 {@code lockToken} 时才移除，
     * 避免误释放其他请求持有的锁。
     *
     * @param sessionId 会话 ID
     * @param lockToken 锁 token（由 {@link #tryLock} 返回）
     */
    public void unlock(Long sessionId, long lockToken) {
        if (lockToken == 0L) {
            return;
        }
        sessionLocks.remove(sessionId, lockToken);
    }

    /**
     * 强制释放会话锁（不受 token 限制）
     * <p>
     * 仅用于会话删除等明确知道锁应当被清理的场景。
     *
     * @param sessionId 会话 ID
     */
    public void forceUnlock(Long sessionId) {
        sessionLocks.remove(sessionId);
    }

    /**
     * 监听会话删除事件，清理对应的锁缓存
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        Long sessionId = event.getSessionId();
        Long removed = sessionLocks.remove(sessionId);
        if (removed != null) {
            log.debug("会话 {} 已删除，清理锁缓存", sessionId);
        }
    }
}
