package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.GlobalCache;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.enums.ConfigType;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.memory.dao.GlobalConfigDao;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Resource
    private InitScheduler initScheduler;

    @Resource
    private ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1000, this::refresh);
    }

    /**
     * 根据枚举获取配置值（JSON 字符串）
     */
    public String getConfig(GlobalConfigKey key) {
        Map<String, String> map = configMap;
        return map != null ? map.get(key.name()) : null;
    }

    /**
     * 获取全量配置 Map（所有类型）
     */
    public Map<String, String> getAllConfigs() {
        return new LinkedHashMap<>(configMap);
    }

    /**
     * 解析并返回 {@link GeneralSettings} DTO。<p>
     * 用于后端各模块便捷读取常规设置，无需自行解析 JSON。
     */
    public GeneralSettings getGeneralSettings() {
        String json = getConfig(GlobalConfigKey.GENERAL_SETTINGS);
        if (json == null || json.isBlank()) {
            return new GeneralSettings();
        }
        try {
            return objectMapper.readValue(json, GeneralSettings.class);
        } catch (Exception e) {
            return new GeneralSettings();
        }
    }

    /**
     * 仅获取前端可见的配置 Map（{@link ConfigType#FRONTEND}）<p>
     * 用于 {@code GET /api/settings/configs} 接口返回给前端。
     */
    public Map<String, String> getFrontendConfigs() {
        Map<String, String> all = configMap;
        if (all == null) {
            return new LinkedHashMap<>();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (GlobalConfigKey key : GlobalConfigKey.values()) {
            if (key.getConfigType() == ConfigType.FRONTEND) {
                String val = all.get(key.name());
                if (val != null) {
                    result.put(key.name(), val);
                }
            }
        }
        return result;
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
