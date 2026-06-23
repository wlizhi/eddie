package cc.wlizhi.eddieai.common.dao;

import cc.wlizhi.eddieai.common.entity.SessionEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
        String sql = "SELECT id, assistant_id, title, pinned, message_count, total_tokens, created_at, updated_at " +
                "FROM ai_session WHERE assistant_id = ? ORDER BY pinned DESC, updated_at DESC";
        return jdbcTemplate.query(sql, sessionRowMapper, assistantId);
    }

    /**
     * 分页查询某助手下的会话列表（置顶 → 更新时间倒序），支持 title 模糊搜索
     *
     * @param assistantId 归属助手 ID
     * @param title       标题模糊搜索关键字（传 null 或空字符串则不过滤）
     * @param offset      偏移量
     * @param limit       每页数量
     */
    public List<SessionEntity> findByAssistantIdPaged(Long assistantId, String title, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, assistant_id, title, pinned, message_count, total_tokens, created_at, updated_at " +
                        "FROM ai_session WHERE assistant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(assistantId);

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title.trim() + "%");
        }

        sql.append(" ORDER BY pinned DESC, updated_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), sessionRowMapper, params.toArray());
    }

    /**
     * 统计某助手下的会话数量，支持 title 模糊过滤
     */
    public long countByAssistantId(Long assistantId, String title) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ai_session WHERE assistant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(assistantId);

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 根据 ID 查询会话
     */
    public SessionEntity findById(Long id) {
        String sql = "SELECT id, assistant_id, title, pinned, message_count, total_tokens, created_at, updated_at FROM ai_session WHERE id = ?";
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
     * 更新活跃时间并同步消息数量和累计 token 数（合并为一条 SQL）
     *
     * @param id         会话 ID
     * @param msgDelta   消息数量增量（正数为增加，负数为减少）
     * @param tokenDelta 累计 token 增量
     */
    public void touchAndIncrementMessageCount(Long id, int msgDelta, int tokenDelta) {
        jdbcTemplate.update(
                "UPDATE ai_session SET updated_at = datetime('now', 'localtime'), " +
                        "message_count = message_count + ?, " +
                        "total_tokens = total_tokens + ? WHERE id = ?",
                msgDelta, tokenDelta, id);
    }

    /**
     * @deprecated 保留兼容，新代码请使用 {@link #touchAndIncrementMessageCount(Long, int, int)}
     */
    @Deprecated
    public void touchAndIncrementMessageCount(Long id, int delta) {
        touchAndIncrementMessageCount(id, delta, 0);
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
        entity.setMessageCount(rs.getInt("message_count"));
        entity.setTotalTokens(rs.getInt("total_tokens"));
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
        entity.setMessageCount(rs.getInt("message_count"));
        entity.setTotalTokens(rs.getInt("total_tokens"));
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        return entity;
    };
}
