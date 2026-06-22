package cc.wlizhi.eddieai.memory.dao;

import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ModelProviderDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询全部服务提供商
     */
    public List<ModelProviderEntity> findAll() {
        String sql = """
                SELECT id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at, updated_at
                FROM model_provider
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ModelProviderEntity.class));
    }

    public ModelProviderEntity getById(Long id) {
        String sql = """
                SELECT id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at, updated_at
                FROM model_provider
                WHERE id = ?
                """;
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(ModelProviderEntity.class), id);
    }
}
