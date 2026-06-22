package cc.wlizhi.eddieai.settings.service.impl;

import cc.wlizhi.eddieai.common.enums.GlobalConfigKey;
import cc.wlizhi.eddieai.memory.context.GlobalConfigContext;
import cc.wlizhi.eddieai.settings.dao.GlobalConfigDao;
import cc.wlizhi.eddieai.settings.service.GlobalConfigService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局配置业务实现
 *
 * @author Eddie
 */
@Service
public class GlobalConfigServiceImpl implements GlobalConfigService {

    private static final Logger log = LoggerFactory.getLogger(GlobalConfigServiceImpl.class);

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private GlobalConfigDao globalConfigDao;

    @Override
    public Map<String, String> getConfigs() {
        return globalConfigContext.getAllConfigs();
    }

    @Override
    public void updateConfigs(Map<String, String> configs) {
        // 过滤非法 key：只保留 GlobalConfigKey 枚举中定义的 key
        Map<String, String> validConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            if (isValidKey(key)) {
                validConfigs.put(key, entry.getValue());
            } else {
                log.warn("忽略非法的全局配置 key: {}", key);
            }
        }

        // 写入 DB（DELETE + batch INSERT，单事务）
        globalConfigDao.replaceAll(validConfigs);

        // 刷新缓存
        globalConfigContext.refresh();
    }

    /**
     * 判断 key 是否在 GlobalConfigKey 枚举中定义
     */
    private boolean isValidKey(String key) {
        if (key == null) {
            return false;
        }
        for (GlobalConfigKey enumKey : GlobalConfigKey.values()) {
            if (enumKey.name().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
