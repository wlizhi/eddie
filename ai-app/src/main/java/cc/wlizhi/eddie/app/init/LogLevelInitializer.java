/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.event.GlobalConfigChangedEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 业务日志级别初始化器。<p>
 * 从 {@link GlobalConfigContext#getGeneralSettings()} 读取 {@link GeneralSettings#getLogLevel()}，
 * 在启动时设置 {@code cc.wlizhi.eddie} 包及子包的日志级别。
 * <p>
 * 通过 {@link InitScheduler} 编排，order=1100 确保在 {@code GlobalConfigContext}（order=1000）缓存就绪后执行。
 *
 * @see GlobalConfigContext
 * @see GeneralSettings
 */
@Slf4j
@Component
public class LogLevelInitializer {

    @Resource
    private InitScheduler initScheduler;

    @Resource
    private GlobalConfigContext globalConfigContext;

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1100, this::doInit);
    }

    private void doInit() {
        GeneralSettings settings = globalConfigContext.getGeneralSettings();
        String levelStr = settings.getLogLevel();
        if (levelStr == null || levelStr.isBlank()) {
            log.info("未配置业务日志级别，使用框架默认值");
            return;
        }
        Level level = Level.toLevel(levelStr, null);
        if (level == null) {
            log.warn("无效的日志级别配置: {}，忽略", levelStr);
            return;
        }
        Logger logger = (Logger) LoggerFactory.getLogger("cc.wlizhi.eddie");
        logger.setLevel(level);
        log.info("业务日志级别已设置为: {}", level);
    }

    /**
     * 监听全局配置变更事件，重新初始化日志级别。<p>
     * 当用户在设置页面修改日志级别后，{@link GlobalConfigContext} 触发 {@code refresh()} 并发布此事件。
     */
    @EventListener
    public void onConfigChanged(GlobalConfigChangedEvent event) {
        log.info("全局配置已变更，重新初始化业务日志级别");
        doInit();
    }

}
