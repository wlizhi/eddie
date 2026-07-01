/**
 * @author Eddie
 * {@code @date} 2026-07-01
 */

package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dao.MessageDao;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 消息状态修复器：应用启动时，将遗留的 STREAMING 状态消息标记为 INTERRUPTED
 * <p>
 * 这些消息是在应用异常关闭时留下的占位 assistant 记录，重启后需要清理。
 */
@Slf4j
@Component
public class MessageStatusFixer {

    @Resource
    private InitScheduler initScheduler;

    @Resource
    private MessageDao messageDao;

    @EventListener(ApplicationReadyEvent.class)
    public void fixStuckMessages() {
        initScheduler.addTask(this.getClass().getSimpleName(), 100, this::doFixStuckMessages);
    }

    private void doFixStuckMessages() {
        try {

            int updated = messageDao.fixStuckMessages();
            if (updated > 0) {
                log.info("修复了 {} 条中断遗留的消息记录（STREAMING → INTERRUPTED）", updated);
            }
        } catch (Exception e) {
            log.warn("修复消息状态时出现异常（不影响正常使用）", e);
        }
    }
}
