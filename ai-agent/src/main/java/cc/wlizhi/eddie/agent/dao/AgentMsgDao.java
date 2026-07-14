/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.common.util.JdbcUtil;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体消息记录 DAO — 操作 ai_agent_session_msg 表（eddie-agent.db）
 */
@Repository
public class AgentMsgDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public Long insert(AgentMsgEntity entity) {
        long now = System.currentTimeMillis();
        return JdbcUtil.insertAndReturnKey(jdbcTemplate,
                "INSERT INTO ai_agent_session_msg " +
                        "(session_id, agent_id, role, provider_id, model_code, model_name, " +
                        "thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, tool_calls, cache_read_input_tokens, cache_written_input_tokens, " +
                        "currency, duration_ms, msg_status, round_seq, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getSessionId(), entity.getAgentId(), entity.getRole(),
                entity.getProviderId(), entity.getModelCode(), entity.getModelName(),
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
                entity.getRoundSeq() != null ? entity.getRoundSeq() : 0,
                now);
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

    /**
     * 更新消息的 task_plan 字段（规划模式持久化规划清单 JSON）
     */
    public void updateTaskPlan(Long id, String taskPlan) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET task_plan = ? WHERE id = ?",
                taskPlan, id);
    }

    /**
     * 绝对赋值更新 token 统计与耗时（每轮迭代结束后调用，覆盖旧值）
     * <p>
     * 与 {@link #updateTokenIncremental} 不同，此方法直接 SET 而非累加，
     * 适用于多轮迭代场景，避免累积值被重复增量写入导致数据膨胀。
     * <p>
     * 使用此方法的前提：调用方传入的值为自请求开始以来的全量累积值。
     */
    public void updateTokenAbsolute(Long id,
                                    int promptTokens, int completionTokens, int totalTokens,
                                    int cacheReadInputTokens, int cacheWriteInputTokens,
                                    String currency, double priceEstimate,
                                    int durationMs) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET " +
                        "prompt_tokens = ?, completion_tokens = ?, total_tokens = ?, " +
                        "cache_read_input_tokens = ?, cache_written_input_tokens = ?, " +
                        "currency = ?, price_estimate = ?, duration_ms = ? " +
                        "WHERE id = ?",
                promptTokens, completionTokens, totalTokens,
                cacheReadInputTokens, cacheWriteInputTokens,
                currency, priceEstimate, durationMs, id);
    }

    /**
     * 更新 AI 回复内容 + 状态（仅更新内容字段，不影响 token 统计）
     * <p>
     * 流式处理结束后调用，写入累积的 thinking/content/toolCalls，
     * 并将 msgStatus 从 PROCESSING 改为 COMPLETED。
     */
    public void updateContentAndStatus(Long id, String content, String thinking, String toolCalls, String msgStatus, Long roundSeq) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET content = ?, thinking = ?, tool_calls = ?, msg_status = ?, round_seq = ? WHERE id = ?",
                content, thinking, toolCalls, msgStatus, roundSeq, id);
    }

    /**
     * 更新消息的 iterator_state（迭代状态快照 JSON）
     */
    public void updateIteratorState(Long id, String iteratorState) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET iterator_state = ? WHERE id = ?",
                iteratorState, id);
    }

    /**
     * 更新消息的 round_seq（回填对话轮次标识）
     */
    public void updateRoundSeq(Long id, Long roundSeq) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg SET round_seq = ? WHERE id = ?",
                roundSeq, id);
    }

    /**
     * 游标分页查询已完成的会话消息（round_seq > 0，跳过占位消息），倒序
     */
    public List<AgentMsgEntity> findBySessionIdCompleted(Long sessionId, Long beforeId, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, session_id, agent_id, role, provider_id, model_code, model_name, " +
                        "thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, tool_calls, cache_read_input_tokens, cache_written_input_tokens, " +
                        "currency, duration_ms, msg_status, round_seq, task_plan, created_at FROM ai_agent_session_msg " +
                        "WHERE session_id = ? AND round_seq > 0");
        List<Object> params = new ArrayList<>();
        params.add(sessionId);

        if (beforeId != null) {
            sql.append(" AND id < ?");
            params.add(beforeId);
        }

        sql.append(" ORDER BY id DESC LIMIT ?");
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    /**
     * 查询会话前 N 轮对话消息，用于生成标题
     *
     * @param sessionId 会话 ID
     * @param rounds    轮数（每轮含 user + assistant 两条消息，取 rounds * 2 条）
     */
    public List<AgentMsgEntity> findRounds(Long sessionId, int rounds) {
        int limit = Math.max(rounds * 2, 2);
        return jdbcTemplate.query(
                "SELECT * FROM ai_agent_session_msg WHERE session_id = ? AND round_seq > 0 ORDER BY id ASC LIMIT ?",
                rowMapper, sessionId, limit);
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
        e.setRole(rs.getString("role"));
        e.setProviderId(rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null);
        e.setModelCode(rs.getString("model_code"));
        e.setModelName(rs.getString("model_name"));
        e.setThinking(rs.getString("thinking"));
        e.setContent(rs.getString("content"));
        e.setPromptTokens(rs.getInt("prompt_tokens"));
        e.setCompletionTokens(rs.getInt("completion_tokens"));
        e.setTotalTokens(rs.getInt("total_tokens"));
        e.setPriceEstimate(rs.getDouble("price_estimate"));
        e.setToolCalls(rs.getString("tool_calls"));
        e.setTaskPlan(rs.getString("task_plan"));
        e.setCacheReadInputTokens(rs.getInt("cache_read_input_tokens"));
        e.setCacheWriteInputTokens(rs.getInt("cache_written_input_tokens"));
        e.setCurrency(rs.getString("currency"));
        e.setDurationMs(rs.getInt("duration_ms"));
        e.setMsgStatus(rs.getString("msg_status"));
        e.setRoundSeq(rs.getLong("round_seq"));
        e.setCreatedAt(rs.getLong("created_at"));
        return e;
    };
}
