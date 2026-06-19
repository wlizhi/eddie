package cc.wlizhi.eddieai.settings.dao;

import cc.wlizhi.eddieai.settings.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ModelProviderMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询全部服务提供商（仅查询接口1需要的字段，不查询 models）
     */
    public List<ModelProviderEntity> findAll() {
        String sql = "SELECT code, name, base_url, api_key, enabled, sort_order, created_at, updated_at FROM model_provider";
        return jdbcTemplate.query(sql, providerRowMapper);
    }

    /**
     * 根据 code 查询 models 字段
     */
    public String findModelsByCode(String code) {
        String sql = "SELECT models FROM model_provider WHERE code = ?";
        List<String> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("models"), code);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 新增服务提供商
     */
    public void insert(ModelProviderEntity entity) {
        String sql = "INSERT INTO model_provider (code, name, base_url, api_key, models, enabled, sort_order, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
        jdbcTemplate.update(sql,
                entity.getCode(),
                entity.getName(),
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModels(),
                entity.getEnabled(),
                entity.getSortOrder());
    }

    /**
     * 更新服务提供商
     */
    public void update(ModelProviderEntity entity) {
        String sql = "UPDATE model_provider SET name = ?, base_url = ?, api_key = ?, models = ?, "
                + "enabled = ?, sort_order = ?, updated_at = datetime('now', 'localtime') WHERE code = ?";
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModels(),
                entity.getEnabled(),
                entity.getSortOrder(),
                entity.getCode());
    }

    /**
     * 根据 code 删除服务提供商
     */
    public void deleteByCode(String code) {
        String sql = "DELETE FROM model_provider WHERE code = ?";
        jdbcTemplate.update(sql, code);
    }

    /**
     * 判断 code 是否存在
     */
    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM model_provider WHERE code = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code);
        return count != null && count > 0;
    }

    private final RowMapper<ModelProviderEntity> providerRowMapper = (rs, rowNum) -> {
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setCode(rs.getString("code"));
        entity.setName(rs.getString("name"));
        entity.setBaseUrl(rs.getString("base_url"));
        entity.setApiKey(rs.getString("api_key"));
        entity.setEnabled(rs.getInt("enabled"));
        entity.setSortOrder(rs.getObject("sort_order") != null ? rs.getInt("sort_order") : null);
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };
}
