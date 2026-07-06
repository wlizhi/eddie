/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 智能体元数据 DAO — 操作 ai_agent 表（eddie-agent.db）
 */
@Repository
public class AgentDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public void insert(AgentEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO ai_agent (name, avatar, description, system_prompt, " +
                        "main_provider_id, main_model_id, main_model_params, " +
                        "sub_provider_id, sub_model_id, sub_model_params, " +
                        "semaphore, max_iterations, max_execution_time_sec, execution_mode, " +
                        "tool_selection_mode, memory_rounds, preferences, enabled, built_in, sort_order, " +
                        "created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getName(), entity.getAvatar(), entity.getDescription(), entity.getSystemPrompt(),
                entity.getMainProviderId(), entity.getMainModelId(), entity.getMainModelParams(),
                entity.getSubProviderId(), entity.getSubModelId(), entity.getSubModelParams(),
                entity.getSemaphore(), entity.getMaxIterations(), entity.getMaxExecutionTimeSec(),
                entity.getExecutionMode(),
                entity.getToolSelectionMode(), entity.getMemoryRounds(), entity.getPreferences(),
                entity.getEnabled(), entity.getBuiltIn(), entity.getSortOrder(),
                now, now);
    }

    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public AgentEntity findById(Long id) {
        String sql = "SELECT * FROM ai_agent WHERE id = ?";
        List<AgentEntity> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询智能体列表
     *
     * @param showAll true=查询全部, false=仅查询启用的
     */
    public List<AgentEntity> findAll(boolean showAll) {
        String sql;
        if (showAll) {
            sql = "SELECT * FROM ai_agent ORDER BY sort_order ASC, created_at DESC";
        } else {
            sql = "SELECT * FROM ai_agent WHERE enabled = 1 ORDER BY sort_order ASC, created_at DESC";
        }
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * 更新智能体
     */
    public void update(AgentEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE ai_agent SET name=?, avatar=?, description=?, system_prompt=?, " +
                        "main_provider_id=?, main_model_id=?, main_model_params=?, " +
                        "sub_provider_id=?, sub_model_id=?, sub_model_params=?, " +
                        "semaphore=?, max_iterations=?, max_execution_time_sec=?, execution_mode=?, " +
                        "tool_selection_mode=?, memory_rounds=?, preferences=?, enabled=?, built_in=?, sort_order=?, " +
                        "updated_at=? WHERE id=?",
                entity.getName(), entity.getAvatar(), entity.getDescription(), entity.getSystemPrompt(),
                entity.getMainProviderId(), entity.getMainModelId(), entity.getMainModelParams(),
                entity.getSubProviderId(), entity.getSubModelId(), entity.getSubModelParams(),
                entity.getSemaphore(), entity.getMaxIterations(), entity.getMaxExecutionTimeSec(),
                entity.getExecutionMode(),
                entity.getToolSelectionMode(), entity.getMemoryRounds(), entity.getPreferences(),
                entity.getEnabled(), entity.getBuiltIn(), entity.getSortOrder(),
                now, entity.getId());
    }

    /**
     * 根据 ID 删除智能体
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM ai_agent WHERE id = ?", id);
    }

    /**
     * 判断 ID 是否存在
     */
    public boolean existsById(Long id) {
        List<Long> results = jdbcTemplate.query(
                "SELECT 1 FROM ai_agent WHERE id = ?",
                (rs, rowNum) -> rs.getLong(1),
                id);
        return !results.isEmpty();
    }

    /**
     * 更新排序序号
     */
    public void updateSortOrder(Long id, int sortOrder) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE ai_agent SET sort_order = ?, updated_at = ? WHERE id = ?",
                sortOrder, now, id);
    }

    private final RowMapper<AgentEntity> rowMapper = (rs, rowNum) -> {
        AgentEntity e = new AgentEntity();
        e.setId(rs.getLong("id"));
        e.setName(rs.getString("name"));
        e.setAvatar(rs.getString("avatar"));
        e.setDescription(rs.getString("description"));
        e.setSystemPrompt(rs.getString("system_prompt"));
        e.setMainProviderId(rs.getObject("main_provider_id") != null ? rs.getLong("main_provider_id") : null);
        e.setMainModelId(rs.getString("main_model_id"));
        e.setMainModelParams(rs.getString("main_model_params"));
        e.setSubProviderId(rs.getObject("sub_provider_id") != null ? rs.getLong("sub_provider_id") : null);
        e.setSubModelId(rs.getString("sub_model_id"));
        e.setSubModelParams(rs.getString("sub_model_params"));
        e.setSemaphore(rs.getInt("semaphore"));
        e.setMaxIterations(rs.getInt("max_iterations"));
        e.setMaxExecutionTimeSec(rs.getInt("max_execution_time_sec"));
        e.setExecutionMode(rs.getString("execution_mode"));
        e.setToolSelectionMode(rs.getString("tool_selection_mode"));
        e.setMemoryRounds(rs.getObject("memory_rounds") != null ? rs.getInt("memory_rounds") : null);
        e.setPreferences(rs.getString("preferences"));
        e.setEnabled(rs.getInt("enabled"));
        e.setBuiltIn(rs.getInt("built_in"));
        e.setSortOrder(rs.getInt("sort_order"));
        e.setCreatedAt(rs.getLong("created_at"));
        e.setUpdatedAt(rs.getLong("updated_at"));
        return e;
    };
}
