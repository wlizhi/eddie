package cc.wlizhi.eddieai.chat.dao;

import cc.wlizhi.eddieai.chat.entity.SessionEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话表数据访问层
 */
@Repository
public class SessionDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建会话
     */
    public void insert(SessionEntity entity) {
        jdbcTemplate.update(
                "INSERT INTO ai_session (assistant_id, title, pinned, created_at, updated_at) " +
                        "VALUES (?, '', 0, datetime('now', 'localtime'), datetime('now', 'localtime'))",
                entity.getAssistantId());
    }

    /**
     * 获取最后插入的自增 ID
     */
    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    /**
     * 查询某助手下的会话列表（置顶 → 更新时间倒序）
     */
    public List<SessionEntity> findByAssistantId(Long assistantId) {
        String sql = "SELECT s.id, s.assistant_id, s.title, s.pinned, s.created_at, s.updated_at, " +
                "(SELECT COUNT(*) FROM ai_session_msg m WHERE m.session_id = s.id) AS message_count " +
                "FROM ai_session s WHERE s.assistant_id = ? " +
                "ORDER BY s.pinned DESC, s.updated_at DESC";
        return jdbcTemplate.query(sql, sessionRowMapper, assistantId);
    }

    /**
     * 根据 ID 查询会话
     */
    public SessionEntity findById(Long id) {
        String sql = "SELECT id, assistant_id, title, pinned, created_at, updated_at FROM ai_session WHERE id = ?";
        List<SessionEntity> results = jdbcTemplate.query(sql, sessionSimpleRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 判断会话是否存在
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_session WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * 更新标题
     */
    public void updateTitle(Long id, String title) {
        jdbcTemplate.update(
                "UPDATE ai_session SET title = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
                title, id);
    }

    /**
     * 更新置顶状态
     */
    public void updatePinned(Long id, int pinned) {
        jdbcTemplate.update(
                "UPDATE ai_session SET pinned = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
                pinned, id);
    }

    /**
     * 更新活跃时间
     */
    public void touch(Long id) {
        jdbcTemplate.update(
                "UPDATE ai_session SET updated_at = datetime('now', 'localtime') WHERE id = ?", id);
    }

    /**
     * 删除会话（消息由应用层先删除）
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM ai_session WHERE id = ?", id);
    }

    /**
     * 删除某助手下的所有会话
     */
    public void deleteByAssistantId(Long assistantId) {
        jdbcTemplate.update("DELETE FROM ai_session WHERE assistant_id = ?", assistantId);
    }

    private final RowMapper<SessionEntity> sessionRowMapper = (rs, rowNum) -> {
        SessionEntity entity = new SessionEntity();
        entity.setId(rs.getLong("id"));
        entity.setAssistantId(rs.getLong("assistant_id"));
        entity.setTitle(rs.getString("title"));
        entity.setPinned(rs.getInt("pinned"));
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };

    private final RowMapper<SessionEntity> sessionSimpleRowMapper = (rs, rowNum) -> {
        SessionEntity entity = new SessionEntity();
        entity.setId(rs.getLong("id"));
        entity.setAssistantId(rs.getLong("assistant_id"));
        entity.setTitle(rs.getString("title"));
        entity.setPinned(rs.getInt("pinned"));
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };
}
