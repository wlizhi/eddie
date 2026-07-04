package cc.wlizhi.eddie.memory.dao;

import cc.wlizhi.eddie.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddie.common.enums.ConfigType;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全局配置表 DAO（只读）
 * <p>
 * 供 {@link GlobalConfigContext} 缓存刷新使用。
 * 写操作在 {@link cc.wlizhi.eddieai.settings.dao.GlobalConfigMapper} 中。
 *
 * @author Eddie
 */
@Repository
public class GlobalConfigDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 查询所有全局配置
     */
    public List<GlobalConfigEntity> findAll() {
        String sql = "SELECT id, config_key, config_val, config_type, description, updated_at FROM global_config";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(GlobalConfigEntity.class));
    }

    /**
     * 逐条 UPSERT 配置。<p>
     * 仅更新传入的 key，其他 key 保持不变，避免误删其他配置。<br>
     * 写入时根据枚举中的 configType 自动填充 config_type 列。
     *
     * @param configs 合法的 key → value 映射
     */
    public void replaceAll(Map<String, String> configs) {
        if (configs.isEmpty()) {
            return;
        }
        transactionTemplate.execute(status -> {
            long now = System.currentTimeMillis();
            String sql = "INSERT OR REPLACE INTO global_config (config_key, config_val, config_type, description, updated_at) "
                    + "VALUES (?, ?, ?, '', ?)";
            List<Object[]> batchArgs = new ArrayList<>();
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String configType = resolveConfigType(entry.getKey());
                batchArgs.add(new Object[]{entry.getKey(), entry.getValue(), configType, now});
            }
            jdbcTemplate.batchUpdate(sql, batchArgs);
            return null;
        });
    }

    /**
     * 根据 configKey 从 GlobalConfigKey 枚举中查找对应的 configType
     */
    private static String resolveConfigType(String configKey) {
        for (GlobalConfigKey key : GlobalConfigKey.values()) {
            if (key.name().equals(configKey)) {
                return key.getConfigType().name();
            }
        }
        return ConfigType.FRONTEND.name();
    }
}
