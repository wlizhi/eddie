/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 智能体消息记录 DAO — 操作 ai_agent_session_msg 表（eddie-agent.db）
 */
@Repository
public class AgentMsgDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public void insert(AgentMsgEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO ai_agent_session_msg " +
                        "(session_id, agent_id, task_id, role, provider_id, model_code, model_name, " +
                        "prompt, thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, tool_calls, cache_read_input_tokens, cache_written_input_tokens, " +
                        "currency, duration_ms, msg_status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getSessionId(), entity.getAgentId(), entity.getTaskId(), entity.getRole(),
                entity.getProviderId(), entity.getModelCode(), entity.getModelName(),
                entity.getPrompt() != null ? entity.getPrompt() : "",
                entity.getThinking() != null ? entity.getThinking() : "", entity.getContent(),
                entity.getPromptTokens() != null ? entity.getPromptTokens() : 0,
                entity.getCompletionTokens() != null ? entity.getCompletionTokens() : 0,
                entity.getTotalTokens() != null ? entity.getTotalTokens() : 0,
                entity.getPriceEstimate() != null ? entity.getPriceEstimate() : 0.0,
                entity.getToolCalls() != null ? entity.getToolCalls() : "[]",
                entity.getCacheReadInputTokens() != null ? entity.getCacheReadInputTokens() : 0,
                entity.getCacheWriteInputTokens() != null ? entity.getCacheWriteInputTokens() : 0,
                entity.getCurrency() != null ? entity.getCurrency() : "",
                entity.getDurationMs() != null ? entity.getDurationMs() : 0,
                entity.getMsgStatus() != null ? entity.getMsgStatus() : "COMPLETED",
                now);
    }

    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    public AgentMsgEntity findById(Long id) {
        String sql = "SELECT * FROM ai_agent_session_msg WHERE id = ?";
        List<AgentMsgEntity> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<AgentMsgEntity> findBySessionId(Long sessionId, Long beforeId, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_agent_session_msg WHERE session_id = ?");
        if (beforeId != null) {
            sql.append(" AND id < ?");
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        if (beforeId != null) {
            return jdbcTemplate.query(sql.toString(), rowMapper, sessionId, beforeId, limit);
        }
        return jdbcTemplate.query(sql.toString(), rowMapper, sessionId, limit);
    }

    public void updateAssistantMsg(Long id, String content, String thinking, String toolCalls,
                                   int promptTokens, int completionTokens, int totalTokens,
                                   int cacheReadInputTokens, int cacheWriteInputTokens,
                                   String currency, double priceEstimate,
                                   String msgStatus, int durationMs) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET content = ?, thinking = ?, tool_calls = ?, " +
                        "prompt_tokens = ?, completion_tokens = ?, total_tokens = ?, " +
                        "cache_read_input_tokens = ?, cache_written_input_tokens = ?, " +
                        "currency = ?, price_estimate = ?, " +
                        "msg_status = ?, duration_ms = ? WHERE id = ?",
                content, thinking, toolCalls,
                promptTokens, completionTokens, totalTokens,
                cacheReadInputTokens, cacheWriteInputTokens,
                currency, priceEstimate,
                msgStatus, durationMs, id);
    }

    /**
     * 增量更新 token 统计（每轮迭代结束后调用，field = field + delta）
     */
    public void updateTokenIncremental(Long id,
                                       int deltaPromptTokens, int deltaCompletionTokens, int deltaTotalTokens,
                                       int deltaCacheReadInputTokens, int deltaCacheWriteInputTokens,
                                       String currency, double priceEstimate,
                                       int deltaDurationMs) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET " +
                        "prompt_tokens = COALESCE(prompt_tokens, 0) + ?, " +
                        "completion_tokens = COALESCE(completion_tokens, 0) + ?, " +
                        "total_tokens = COALESCE(total_tokens, 0) + ?, " +
                        "cache_read_input_tokens = COALESCE(cache_read_input_tokens, 0) + ?, " +
                        "cache_written_input_tokens = COALESCE(cache_written_input_tokens, 0) + ?, " +
                        "currency = ?, " +
                        "price_estimate = COALESCE(price_estimate, 0) + ?, " +
                        "duration_ms = COALESCE(duration_ms, 0) + ?, " +
                        "msg_status = 'COMPLETED' WHERE id = ?",
                deltaPromptTokens, deltaCompletionTokens, deltaTotalTokens,
                deltaCacheReadInputTokens, deltaCacheWriteInputTokens,
                currency, priceEstimate, deltaDurationMs, id);
    }

    /**
     * 更新 AI 回复内容 + 状态（仅更新内容字段，不影响 token 统计）
     * <p>
     * 流式处理结束后调用，写入累积的 thinking/content/toolCalls，
     * 并将 msgStatus 从 PROCESSING 改为 COMPLETED。
     */
    public void updateContentAndStatus(Long id, String content, String thinking, String toolCalls, String msgStatus) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET content = ?, thinking = ?, tool_calls = ?, msg_status = ? WHERE id = ?",
                content, thinking, toolCalls, msgStatus, id);
    }

    /**
     * 根据智能体 ID 删除所有消息（级联删除用）
     */
    public void deleteByAgentId(Long agentId) {
        jdbcTemplate.update("DELETE FROM ai_agent_session_msg WHERE agent_id = ?", agentId);
    }

    /**
     * 根据会话 ID 删除所有消息
     */
    public void deleteBySessionId(Long sessionId) {
        jdbcTemplate.update("DELETE FROM ai_agent_session_msg WHERE session_id = ?", sessionId);
    }

    private final RowMapper<AgentMsgEntity> rowMapper = (rs, rowNum) -> {
        AgentMsgEntity e = new AgentMsgEntity();
        e.setId(rs.getLong("id"));
        e.setSessionId(rs.getLong("session_id"));
        e.setAgentId(rs.getLong("agent_id"));
        e.setTaskId(rs.getObject("task_id") != null ? rs.getLong("task_id") : null);
        e.setRole(rs.getString("role"));
        e.setProviderId(rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null);
        e.setModelCode(rs.getString("model_code"));
        e.setModelName(rs.getString("model_name"));
        e.setPrompt(rs.getString("prompt"));
        e.setThinking(rs.getString("thinking"));
        e.setContent(rs.getString("content"));
        e.setPromptTokens(rs.getInt("prompt_tokens"));
        e.setCompletionTokens(rs.getInt("completion_tokens"));
        e.setTotalTokens(rs.getInt("total_tokens"));
        e.setPriceEstimate(rs.getDouble("price_estimate"));
        e.setToolCalls(rs.getString("tool_calls"));
        e.setCacheReadInputTokens(rs.getInt("cache_read_input_tokens"));
        e.setCacheWriteInputTokens(rs.getInt("cache_written_input_tokens"));
        e.setCurrency(rs.getString("currency"));
        e.setDurationMs(rs.getInt("duration_ms"));
        e.setMsgStatus(rs.getString("msg_status"));
        e.setCreatedAt(rs.getLong("created_at"));
        return e;
    };
}
