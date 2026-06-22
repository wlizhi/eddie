package cc.wlizhi.eddieai.settings.service;

import java.util.Map;

/**
 * 全局配置业务接口
 *
 * @author Eddie
 */
public interface GlobalConfigService {

    /**
     * 获取全部全局配置
     */
    Map<String, String> getConfigs();

    /**
     * 全量更新全局配置。<p>
     * 会过滤掉不在 {@link cc.wlizhi.eddieai.common.enums.GlobalConfigKey} 中的非法 key。<br>
     * 更新后自动刷新缓存。
     */
    void updateConfigs(Map<String, String> configs);
}
