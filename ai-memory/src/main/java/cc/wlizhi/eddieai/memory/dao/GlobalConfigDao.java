package cc.wlizhi.eddieai.memory.dao;

import cc.wlizhi.eddieai.common.entity.GlobalConfigEntity;
import jakarta.annotation.Resource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
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
 * 供 {@link cc.wlizhi.eddieai.memory.context.GlobalConfigContext} 缓存刷新使用。
 * 写操作在 {@link cc.wlizhi.eddieai.settings.dao.GlobalConfigMapper} 中。
 *
 * @author Eddie
 */
@RegisterReflectionForBinding(GlobalConfigEntity.class)
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
        String sql = "SELECT id, config_key, config_val, description, updated_at FROM global_config";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(GlobalConfigEntity.class));
    }

    /**
     * 全量替换所有配置。<p>
     * DELETE 旧数据 → batch INSERT 新数据，在同一事务中执行。
     *
     * @param configs 合法的 key → value 映射
     */
    public void replaceAll(Map<String, String> configs) {
        transactionTemplate.execute(status -> {
            // 清空旧数据（SQLite 无 TRUNCATE，DELETE FROM 等效）
            jdbcTemplate.update("DELETE FROM global_config");

            // 2. 批量插入新数据
            String sql = "INSERT INTO global_config (config_key, config_val, description, updated_at) "
                    + "VALUES (?, ?, ?, datetime('now', 'localtime'))";
            List<Object[]> batchArgs = new ArrayList<>();
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                batchArgs.add(new Object[]{entry.getKey(), entry.getValue(), ""});
            }
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }
            return null;
        });
    }
}
