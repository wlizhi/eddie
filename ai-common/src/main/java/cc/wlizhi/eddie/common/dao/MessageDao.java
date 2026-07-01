/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.MessageEntity;
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
                        "total_tokens, price_estimate, tool_calls, " +
                        "cache_read_input_tokens, cache_written_input_tokens, currency, duration_ms, " +
                        "msg_status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))",
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
                entity.getPriceEstimate(),
                entity.getToolCalls(),
                entity.getCacheReadInputTokens() != null ? entity.getCacheReadInputTokens() : 0,
                entity.getCacheWriteInputTokens() != null ? entity.getCacheWriteInputTokens() : 0,
                entity.getCurrency() != null ? entity.getCurrency() : "",
                entity.getDurationMs() != null ? entity.getDurationMs() : 0,
                entity.getMsgStatus() != null ? entity.getMsgStatus() : "COMPLETED");
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
                        "price_estimate, tool_calls, cache_read_input_tokens, cache_written_input_tokens, " +
                        "currency, duration_ms, created_at FROM ai_session_msg WHERE session_id = ?");
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
     * 查询会话前 N 轮对话消息，用于生成标题
     *
     * @param sessionId 会话 ID
     * @param rounds    轮数（每轮含 user + assistant 两条消息，取 rounds * 2 条）
     */
    public List<MessageEntity> findRounds(Long sessionId, int rounds) {
        int limit = Math.max(rounds * 2, 2);
        return jdbcTemplate.query(
                "SELECT id, session_id, assistant_id, role, provider_id, model_code, model_name, " +
                        "thinking, content, prompt_tokens, completion_tokens, total_tokens, " +
                        "price_estimate, tool_calls, cache_read_input_tokens, cache_written_input_tokens, " +
                        "currency, duration_ms, created_at FROM ai_session_msg WHERE session_id = ? " +
                        "ORDER BY id ASC LIMIT ?",
                messageRowMapper, sessionId, limit);
    }

    /**
     * 获取最后插入的自增 ID（仅在 INSERT 后立即调用有效）
     */
    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
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

    /**
     * 更新 assistant 消息内容（流结束后或中断时调用，从占位符更新为实际内容）
     *
     * @param id                    消息 ID
     * @param content               回答内容
     * @param thinking              思考内容
     * @param toolCalls             工具调用记录 JSON
     * @param promptTokens          提示 token 数
     * @param completionTokens      完成 token 数
     * @param totalTokens           总 token 数
     * @param cacheReadInputTokens  缓存读取 token
     * @param cacheWriteInputTokens 缓存写入 token
     * @param currency              货币符号
     * @param priceEstimate         预估费用
     * @param durationMs            耗时（毫秒）
     * @param msgStatus             消息状态：COMPLETED / INTERRUPTED
     */
    public void updateAssistantMsg(Long id, String content, String thinking, String toolCalls,
                                   int promptTokens, int completionTokens, int totalTokens,
                                   int cacheReadInputTokens, int cacheWriteInputTokens,
                                   String currency, double priceEstimate, int durationMs,
                                   String msgStatus) {
        jdbcTemplate.update(
                "UPDATE ai_session_msg SET content = ?, thinking = ?, tool_calls = ?, " +
                        "prompt_tokens = ?, completion_tokens = ?, total_tokens = ?, " +
                        "cache_read_input_tokens = ?, cache_written_input_tokens = ?, " +
                        "currency = ?, price_estimate = ?, duration_ms = ?, " +
                        "msg_status = ? WHERE id = ?",
                content, thinking, toolCalls,
                promptTokens, completionTokens, totalTokens,
                cacheReadInputTokens, cacheWriteInputTokens,
                currency, priceEstimate, durationMs,
                msgStatus, id);
    }

    /**
     * 修复 stuck 消息：将长时间处于 STREAMING 状态的消息标记为 INTERRUPTED
     * <p>
     * 仅修复最近 7 天内的 stuck 记录，避免全表扫描和历史数据堆积。
     * 在应用启动时或首次查询时调用。
     */
    public int fixStuckMessages() {
        return jdbcTemplate.update(
                "UPDATE ai_session_msg SET msg_status = 'INTERRUPTED' " +
                        "WHERE msg_status = 'STREAMING' " +
                        "  AND created_at >= datetime('now', '-7 days', 'localtime')");
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
        entity.setToolCalls(rs.getString("tool_calls"));
        entity.setCacheReadInputTokens(rs.getInt("cache_read_input_tokens"));
        entity.setCacheWriteInputTokens(rs.getInt("cache_written_input_tokens"));
        entity.setCurrency(rs.getString("currency"));
        entity.setDurationMs(rs.getInt("duration_ms"));
        entity.setMsgStatus(rs.getString("msg_status"));
        entity.setCreatedAt(rs.getString("created_at"));
        return entity;
    };
}
