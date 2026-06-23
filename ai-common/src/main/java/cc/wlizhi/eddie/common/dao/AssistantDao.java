package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.AssistantEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 助手列表数据访问层
 */
@Repository
public class AssistantDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询助手列表
     *
     * @param showAll true=查询全部, false=仅查询启用的
     */
    public List<AssistantEntity> findAll(boolean showAll) {
        String sql = """
                SELECT a.id, a.name, a.avatar, a.description, a.system_prompt,
                       a.provider_id, a.model_id, a.model_params, a.memory_rounds,
                       a.enabled, a.sort_order, a.created_at, a.updated_at
                FROM ai_assistant a
                """;
        if (!showAll) {
            sql += " WHERE a.enabled = 1";
        }
        sql += " ORDER BY a.enabled DESC, a.sort_order ASC, a.id ASC";
        return jdbcTemplate.query(sql, assistantRowMapper);
    }

    /**
     * 根据 ID 查询助手（LEFT JOIN model_provider 获取服务商名称）
     */
    public AssistantEntity findById(Long id) {
        String sql = """
                SELECT a.id, a.name, a.avatar, a.description, a.system_prompt,
                       a.provider_id, a.model_id, a.model_params, a.memory_rounds,
                       a.enabled, a.sort_order, a.created_at, a.updated_at
                FROM ai_assistant a
                WHERE a.id = ?
                """;
        List<AssistantEntity> results = jdbcTemplate.query(sql, assistantWithProviderRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 新增助手
     */
    public void insert(AssistantEntity entity) {
        String sql = """
                INSERT INTO ai_assistant (name, avatar, description, system_prompt,
                                          provider_id, model_id, model_params, memory_rounds,
                                          enabled, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        datetime('now', 'localtime'), datetime('now', 'localtime'))
                """;
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getAvatar(),
                entity.getDescription(),
                entity.getSystemPrompt(),
                entity.getProviderId(),
                entity.getModelId(),
                entity.getModelParams(),
                entity.getMemoryRounds(),
                entity.getEnabled(),
                entity.getSortOrder());
    }

    /**
     * 更新助手（动态 SQL，仅更新非 null 字段）
     */
    public void update(AssistantEntity entity) {
        List<Object> params = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE ai_assistant SET updated_at = datetime('now', 'localtime')");

        if (entity.getName() != null) {
            sql.append(", name = ?");
            params.add(entity.getName());
        }
        if (entity.getAvatar() != null) {
            sql.append(", avatar = ?");
            params.add(entity.getAvatar());
        }
        if (entity.getDescription() != null) {
            sql.append(", description = ?");
            params.add(entity.getDescription());
        }
        if (entity.getSystemPrompt() != null) {
            sql.append(", system_prompt = ?");
            params.add(entity.getSystemPrompt());
        }
        if (entity.getProviderId() != null) {
            sql.append(", provider_id = ?");
            params.add(entity.getProviderId());
        }
        if (entity.getModelId() != null) {
            sql.append(", model_id = ?");
            params.add(entity.getModelId());
        }
        if (entity.getModelParams() != null) {
            sql.append(", model_params = ?");
            params.add(entity.getModelParams());
        }
        if (entity.getMemoryRounds() != null) {
            sql.append(", memory_rounds = ?");
            params.add(entity.getMemoryRounds());
        }
        if (entity.getEnabled() != null) {
            sql.append(", enabled = ?");
            params.add(entity.getEnabled());
        }
        if (entity.getSortOrder() != null) {
            sql.append(", sort_order = ?");
            params.add(entity.getSortOrder());
        }

        sql.append(" WHERE id = ?");
        params.add(entity.getId());

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * 更新助手头像字段
     */
    public void updateAvatar(Long id, String avatar) {
        jdbcTemplate.update(
                "UPDATE ai_assistant SET avatar = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
                avatar, id);
    }

    /**
     * 更新单个助手的排序序号
     */
    public void updateSortOrder(Long id, int sortOrder) {
        jdbcTemplate.update(
                "UPDATE ai_assistant SET sort_order = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
                sortOrder, id);
    }

    /**
     * 物理删除助手
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM ai_assistant WHERE id = ?", id);
    }

    /**
     * 判断 ID 是否存在
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_assistant WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * 获取最后插入的自增 ID（SQLite）
     */
    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    /**
     * 自定义 RowMapper：仅映射 AssistantEntity 基础字段
     */
    private final RowMapper<AssistantEntity> assistantRowMapper = (rs, rowNum) -> {
        AssistantEntity entity = new AssistantEntity();
        entity.setId(rs.getLong("id"));
        entity.setName(rs.getString("name"));
        entity.setAvatar(rs.getString("avatar"));
        entity.setDescription(rs.getString("description"));
        entity.setSystemPrompt(rs.getString("system_prompt"));
        entity.setProviderId(rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null);
        entity.setModelId(rs.getString("model_id"));
        entity.setModelParams(rs.getString("model_params"));
        entity.setMemoryRounds(rs.getInt("memory_rounds"));
        entity.setEnabled(rs.getInt("enabled"));
        entity.setSortOrder(rs.getObject("sort_order") != null ? rs.getInt("sort_order") : 0);
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };

    /**
     * 自定义 RowMapper：映射 AssistantEntity + JOIN 字段
     */
    private final RowMapper<AssistantEntity> assistantWithProviderRowMapper = (rs, rowNum) -> {
        AssistantEntity entity = new AssistantEntity();
        entity.setId(rs.getLong("id"));
        entity.setName(rs.getString("name"));
        entity.setAvatar(rs.getString("avatar"));
        entity.setDescription(rs.getString("description"));
        entity.setSystemPrompt(rs.getString("system_prompt"));
        entity.setProviderId(rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null);
        entity.setModelId(rs.getString("model_id"));
        entity.setModelParams(rs.getString("model_params"));
        entity.setMemoryRounds(rs.getInt("memory_rounds"));
        entity.setEnabled(rs.getInt("enabled"));
        entity.setSortOrder(rs.getObject("sort_order") != null ? rs.getInt("sort_order") : 0);
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };
}
