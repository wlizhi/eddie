/**
 * @author Eddie
 * {@code @date} 2026-07-01
 */

package cc.wlizhi.eddie.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 会话删除事件
 * <p>
 * 当会话被删除时发布，供 {@link cc.wlizhi.eddie.common.cache.SessionLockManager} 等监听者
 * 清理该会话相关的缓存资源。
 */
@Getter
public class SessionDeletedEvent extends ApplicationEvent {

    private final Long sessionId;

    /**
     * @param source    事件源（通常为发布事件的 bean）
     * @param sessionId 被删除的会话 ID
     */
    public SessionDeletedEvent(Object source, Long sessionId) {
        super(source);
        this.sessionId = sessionId;
    }

}
