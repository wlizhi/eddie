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
     * 查询全部服务提供商（仅查询接口1需要的字段，不查询 models）
     */
    public List<ModelProviderEntity> findAll() {
        String sql = """
                SELECT code, name, base_url, api_key, enabled, sort_order, created_at, updated_at
                FROM model_provider
                ORDER BY sort_order ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ModelProviderEntity.class));
    }
}
