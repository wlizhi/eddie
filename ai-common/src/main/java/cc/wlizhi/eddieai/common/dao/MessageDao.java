package cc.wlizhi.eddieai.common.dao;

import cc.wlizhi.eddieai.common.entity.MessageEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息记录表数据访问层
 */
@Repository
public class MessageDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 插入消息
     */
    public void insert(MessageEntity entity) {
        jdbcTemplate.update(
                "INSERT INTO ai_session_msg (session_id, assistant_id, role, provider_id, " +
                        "model_code, model_name, thinking, content, prompt_tokens, completion_tokens, " +
                        "total_tokens, price_estimate, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))",
                entity.getSessionId(),
                entity.getAssistantId(),
                entity.getRole(),
                entity.getProviderId(),
                entity.getModelCode(),
                entity.getModelName(),
                entity.getThinking(),
                entity.getContent(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getPriceEstimate());
    }

    /**
     * 游标分页查询会话消息（倒序，最新在前）
     *
     * @param sessionId 会话 ID
     * @param beforeId  游标：已加载的最早消息 ID，首次传 null
     * @param limit     每页数量
     */
    public List<MessageEntity> findBySessionId(Long sessionId, Long beforeId, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, session_id, assistant_id, role, provider_id, model_code, model_name, " +
                        "thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, created_at FROM ai_session_msg WHERE session_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(sessionId);

        if (beforeId != null) {
            sql.append(" AND id < ?");
            params.add(beforeId);
        }

        sql.append(" ORDER BY id DESC LIMIT ?");
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), messageRowMapper, params.toArray());
    }

    /**
     * 查询会话首轮对话（前 2 条消息），用于生成标题
     */
    public List<MessageEntity> findFirstRound(Long sessionId) {
        return jdbcTemplate.query(
                "SELECT id, session_id, assistant_id, role, provider_id, model_code, model_name, " +
                        "thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, created_at FROM ai_session_msg WHERE session_id = ? " +
                        "ORDER BY id ASC LIMIT 2",
                messageRowMapper, sessionId);
    }

    /**
     * 统计会话消息数
     */
    public int countBySessionId(Long sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_session_msg WHERE session_id = ?", Integer.class, sessionId);
        return count != null ? count : 0;
    }

    /**
     * 删除会话的所有消息
     */
    public void deleteBySessionId(Long sessionId) {
        jdbcTemplate.update("DELETE FROM ai_session_msg WHERE session_id = ?", sessionId);
    }

    /**
     * 删除某助手下的所有消息
     */
    public void deleteByAssistantId(Long assistantId) {
        jdbcTemplate.update("DELETE FROM ai_session_msg WHERE assistant_id = ?", assistantId);
    }

    private final RowMapper<MessageEntity> messageRowMapper = (rs, rowNum) -> {
        MessageEntity entity = new MessageEntity();
        entity.setId(rs.getLong("id"));
        entity.setSessionId(rs.getLong("session_id"));
        entity.setAssistantId(rs.getLong("assistant_id"));
        entity.setRole(rs.getString("role"));
        entity.setProviderId(rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null);
        entity.setModelCode(rs.getString("model_code"));
        entity.setModelName(rs.getString("model_name"));
        entity.setThinking(rs.getString("thinking"));
        entity.setContent(rs.getString("content"));
        entity.setPromptTokens(rs.getInt("prompt_tokens"));
        entity.setCompletionTokens(rs.getInt("completion_tokens"));
        entity.setTotalTokens(rs.getInt("total_tokens"));
        entity.setPriceEstimate(rs.getDouble("price_estimate"));
        entity.setCreatedAt(rs.getString("created_at"));
        return entity;
    };
}
