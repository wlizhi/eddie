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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Session 级别的互斥锁管理器
 * <p>
 * 确保同一会话同时只有一个流式请求在处理，避免并发导致的上下文错乱和消息顺序问题。
 * <p>
 * 锁的生命周期为应用启动到关闭之间，{@link ConcurrentHashMap} 存储活跃会话的锁实例。
 * 当会话被删除时，通过监听 {@link SessionDeletedEvent} 清理对应的锁缓存，避免内存泄漏。
 */
@Component
public class SessionLockManager {

    private static final Logger log = LoggerFactory.getLogger(SessionLockManager.class);

    private final ConcurrentHashMap<Long, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 尝试获取会话锁
     *
     * @param sessionId 会话 ID
     * @return true 表示获取成功，该会话可继续处理；false 表示该会话正在被其他请求处理
     */
    public boolean tryLock(Long sessionId) {
        ReentrantLock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        boolean acquired = lock.tryLock();
        if (!acquired) {
            log.warn("会话 {} 正在处理中，拒绝并发请求", sessionId);
        }
        return acquired;
    }

    /**
     * 释放会话锁
     *
     * @param sessionId 会话 ID
     */
    public void unlock(Long sessionId) {
        ReentrantLock lock = sessionLocks.get(sessionId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 监听会话删除事件，清理对应的锁缓存
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        Long sessionId = event.getSessionId();
        ReentrantLock removed = sessionLocks.remove(sessionId);
        if (removed != null) {
            log.debug("会话 {} 已删除，清理锁缓存", sessionId);
        }
    }
}
