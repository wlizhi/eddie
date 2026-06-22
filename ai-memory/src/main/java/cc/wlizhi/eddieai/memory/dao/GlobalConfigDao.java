package cc.wlizhi.eddieai.memory.dao;

import cc.wlizhi.eddieai.common.entity.GlobalConfigEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 全局配置表 DAO（只读）
 * <p>
 * 供 {@link cc.wlizhi.eddieai.memory.context.GlobalConfigContext} 缓存刷新使用。
 * 写操作在 {@link cc.wlizhi.eddieai.settings.dao.GlobalConfigMapper} 中。
 *
 * @author Eddie
 */
@Repository
public class GlobalConfigDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询所有全局配置
     */
    public List<GlobalConfigEntity> findAll() {
        String sql = "SELECT id, config_key, config_val, description, updated_at FROM global_config";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(GlobalConfigEntity.class));
    }
}
