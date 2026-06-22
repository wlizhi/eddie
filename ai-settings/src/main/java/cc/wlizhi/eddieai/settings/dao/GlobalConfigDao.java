package cc.wlizhi.eddieai.settings.dao;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全局配置表 Mapper（写操作）
 * <p>
 * 全量替换方式，供 {@link cc.wlizhi.eddieai.settings.service.GlobalConfigService} 使用。
 * 读取缓存由 {@link cc.wlizhi.eddieai.memory.context.GlobalConfigContext} 负责。
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
