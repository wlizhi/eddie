package cc.wlizhi.eddieai.memory.context;

import cc.wlizhi.eddieai.common.cache.GlobalCache;
import cc.wlizhi.eddieai.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddieai.common.enums.GlobalConfigKey;
import cc.wlizhi.eddieai.memory.dao.GlobalConfigDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 全局配置缓存上下文。<p>
 * 全应用生命周期缓存，以 {@code configKey} 为键存储 JSON 字符串。<br>
 * 写入时通过 {@link #refresh()} 刷新缓存，前端打开设置页面或修改配置后触发。
 *
 * @author Eddie
 */
@Component
public class GlobalConfigContext implements GlobalCache {

    private volatile Map<String, String> configMap;
    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private GlobalConfigDao globalConfigDao;

    @PostConstruct
    void init() {
        refresh();
    }

    /**
     * 根据枚举获取配置值（JSON 字符串）
     */
    public String getConfig(GlobalConfigKey key) {
        Map<String, String> map = configMap;
        return map != null ? map.get(key.name()) : null;
    }

    /**
     * 获取指定枚举的配置值，并反序列化为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(GlobalConfigKey key, Class<T> valueType) {
        String json = getConfig(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        // 如果目标类型是 String，直接返回
        if (valueType == String.class) {
            return (T) json;
        }
        // 简单 JSON 反序列化，使用 Jackson
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, valueType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取全量配置 Map（前端用）
     */
    public Map<String, String> getAllConfigs() {
        return new LinkedHashMap<>(configMap);
    }

    @Override
    public void refresh() {
        try {
            lock.lock();
            List<GlobalConfigEntity> list = globalConfigDao.findAll();
            // 先转成 DB map
            Map<String, String> dbMap = new LinkedHashMap<>();
            for (GlobalConfigEntity entity : list) {
                dbMap.put(entity.getConfigKey(), entity.getConfigVal());
            }
            // 按枚举顺序过滤，忽略脏数据
            Map<String, String> map = new LinkedHashMap<>();
            for (GlobalConfigKey key : GlobalConfigKey.values()) {
                String val = dbMap.get(key.name());
                if (val != null) {
                    map.put(key.name(), val);
                }
            }
            this.configMap = map;
        } finally {
            lock.unlock();
        }
    }
}
