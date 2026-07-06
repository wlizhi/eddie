/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.memory.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 全局配置变更事件。<p>
 * 当 {@link cc.wlizhi.eddie.memory.context.GlobalConfigContext#refresh()} 完成缓存刷新后发布，<br>
 * 携带全量配置 Map，供各监听方（如 {@link cc.wlizhi.eddie.app.init.LogLevelInitializer}）按需响应。
 *
 * @see cc.wlizhi.eddie.memory.context.GlobalConfigContext
 */
public class GlobalConfigChangedEvent extends ApplicationEvent {

    private final Map<String, String> configMap;

    /**
     * @param source    事件源（通常为 GlobalConfigContext 实例）
     * @param configMap 刷新后的全量配置 Map
     */
    public GlobalConfigChangedEvent(Object source, Map<String, String> configMap) {
        super(source);
        this.configMap = configMap;
    }

    /**
     * 获取刷新后的全量配置 Map。
     */
    public Map<String, String> getConfigMap() {
        return configMap;
    }
}
