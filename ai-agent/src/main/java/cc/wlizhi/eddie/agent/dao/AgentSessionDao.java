/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体会话 DAO — 操作 ai_agent_session 表（eddie-agent.db）
 */
@Repository
public class AgentSessionDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public void insert(AgentSessionEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO ai_agent_session (agent_id, title, pinned, created_at, updated_at) " +
                        "VALUES (?, '', 0, ?, ?)",
                entity.getAgentId(), now, now);
    }

    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public AgentSessionEntity findById(Long id) {
        String sql = "SELECT id, agent_id, title, pinned, message_count, total_tokens, created_at, updated_at " +
                "FROM ai_agent_session WHERE id = ?";
        List<AgentSessionEntity> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 判断会话是否存在
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_agent_session WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public List<AgentSessionEntity> findByAgentId(Long agentId) {
        String sql = "SELECT id, agent_id, title, pinned, message_count, total_tokens, created_at, updated_at " +
                "FROM ai_agent_session WHERE agent_id = ? ORDER BY pinned DESC, updated_at DESC";
        return jdbcTemplate.query(sql, rowMapper, agentId);
    }

    /**
     * 分页查询某智能体下的会话列表，支持 title 模糊搜索
     */
    public List<AgentSessionEntity> findByAgentIdPaged(Long agentId, String title, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, agent_id, title, pinned, message_count, total_tokens, created_at, updated_at " +
                        "FROM ai_agent_session WHERE agent_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(agentId);

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title.trim() + "%");
        }

        sql.append(" ORDER BY pinned DESC, updated_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    /**
     * 统计某智能体下的会话数量，支持 title 模糊过滤
     */
    public long countByAgentId(Long agentId, String title) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ai_agent_session WHERE agent_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(agentId);

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 更新置顶状态
     */
    public void updatePinned(Long id, int pinned) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE ai_agent_session SET pinned = ?, updated_at = ? WHERE id = ?",
                pinned, now, id);
    }

    public void touchAndIncrementMessageCount(Long id, int msgDelta, int tokenDelta) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE ai_agent_session SET updated_at = ?, " +
                        "message_count = message_count + ?, " +
                        "total_tokens = total_tokens + ? WHERE id = ?",
                now, msgDelta, tokenDelta, id);
    }

    public void updateTitle(Long id, String title) {
        jdbcTemplate.update("UPDATE ai_agent_session SET title = ? WHERE id = ?", title, id);
    }

    /**
     * 删除单个会话
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM ai_agent_session WHERE id = ?", id);
    }

    /**
     * 根据智能体 ID 删除所有会话
     */
    public void deleteByAgentId(Long agentId) {
        jdbcTemplate.update("DELETE FROM ai_agent_session WHERE agent_id = ?", agentId);
    }

    private final RowMapper<AgentSessionEntity> rowMapper = (rs, rowNum) -> {
        AgentSessionEntity e = new AgentSessionEntity();
        e.setId(rs.getLong("id"));
        e.setAgentId(rs.getLong("agent_id"));
        e.setTitle(rs.getString("title"));
        e.setPinned(rs.getInt("pinned"));
        e.setMessageCount(rs.getInt("message_count"));
        e.setTotalTokens(rs.getInt("total_tokens"));
        e.setCreatedAt(rs.getLong("created_at"));
        e.setUpdatedAt(rs.getLong("updated_at"));
        return e;
    };
}
