package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@RegisterReflectionForBinding(ModelProviderEntity.class)
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

    /**
     * 根据 id 查询
     */
    public ModelProviderEntity findById(Long id) {
        String sql = "SELECT id, code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at, updated_at FROM model_provider WHERE id = ?";
        List<ModelProviderEntity> results = jdbcTemplate.query(sql, providerRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
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
        String sql = "INSERT INTO model_provider (code, name, base_url, api_key, models, enabled, built_in, sort_order, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
        jdbcTemplate.update(sql,
                entity.getCode(),
                entity.getName(),
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModels(),
                entity.getEnabled(),
                entity.getBuiltIn(),
                entity.getSortOrder());
    }

    /**
     * 按 id 更新服务提供商
     */
    public void update(ModelProviderEntity entity) {
        String sql = "UPDATE model_provider SET name = ?, base_url = ?, api_key = ?, models = ?, "
                + "enabled = ?, sort_order = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModels(),
                entity.getEnabled(),
                entity.getSortOrder(),
                entity.getId());
    }

    /**
     * 根据 id 删除服务提供商
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM model_provider WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 判断 code 是否存在
     */
    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM model_provider WHERE code = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code);
        return count != null && count > 0;
    }

    /**
     * 更新排序序号
     */
    public void updateSortOrder(Long id, int sortOrder) {
        String sql = "UPDATE model_provider SET sort_order = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        jdbcTemplate.update(sql, sortOrder, id);
    }

    private final RowMapper<ModelProviderEntity> providerRowMapper = (rs, rowNum) -> {
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setId(rs.getLong("id"));
        entity.setCode(rs.getString("code"));
        entity.setName(rs.getString("name"));
        entity.setBaseUrl(rs.getString("base_url"));
        entity.setApiKey(rs.getString("api_key"));
        entity.setModels(rs.getString("models"));
        entity.setEnabled(rs.getInt("enabled"));
        entity.setBuiltIn(rs.getInt("built_in"));
        entity.setSortOrder(rs.getObject("sort_order") != null ? rs.getInt("sort_order") : null);
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };
}
