/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

import java.util.List;

/**
 * 消息分段明细 DAO — 操作 ai_agent_session_msg_step 表（eddie-agent.db）
 */
@Repository
public class AgentMsgStepDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public void insert(AgentMsgStepEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO ai_agent_session_msg_step " +
                        "(msg_id, msg_type, msg_data_type, step, step_desc, " +
                        "prompt, thinking, content, tool_calls, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getMsgId(),
                entity.getMsgType() != null ? entity.getMsgType() : 0,
                entity.getMsgDataType() != null ? entity.getMsgDataType() : 0,
                entity.getStep() != null ? entity.getStep() : 0,
                entity.getStepDesc() != null ? entity.getStepDesc() : "",
                entity.getPrompt() != null ? entity.getPrompt() : "",
                entity.getThinking() != null ? entity.getThinking() : "",
                entity.getContent() != null ? entity.getContent() : "",
                entity.getToolCalls() != null ? entity.getToolCalls() : "[]",
                now);
    }

    /**
     * 预创建占位记录，返回自增 ID。
     * content/thinking/toolCalls 为空，流结束后通过 {@link #updateContent} 填充。
     */
    public Long insertPlaceholder(AgentMsgStepEntity entity) {
        long now = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ai_agent_session_msg_step " +
                            "(msg_id, msg_type, msg_data_type, step, step_desc, " +
                            "prompt, thinking, content, tool_calls, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, '', '', '[]', ?)",
                    new String[]{"id"});
            ps.setLong(1, entity.getMsgId());
            ps.setInt(2, entity.getMsgType() != null ? entity.getMsgType() : 0);
            ps.setInt(3, entity.getMsgDataType() != null ? entity.getMsgDataType() : 0);
            ps.setInt(4, entity.getStep() != null ? entity.getStep() : 0);
            ps.setString(5, entity.getStepDesc() != null ? entity.getStepDesc() : "");
            ps.setString(6, entity.getPrompt() != null ? entity.getPrompt() : "");
            ps.setLong(7, now);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    /**
     * 流结束后更新占位记录的实际内容
     */
    public void updateContent(Long id, String content, String thinking, String toolCalls) {
        jdbcTemplate.update(
                "UPDATE ai_agent_session_msg_step SET content=?, thinking=?, tool_calls=? WHERE id=?",
                content != null ? content : "",
                thinking != null ? thinking : "",
                toolCalls != null ? toolCalls : "[]",
                id);
    }

    public List<AgentMsgStepEntity> findByMsgId(Long msgId) {
        String sql = "SELECT id, msg_id, msg_type, msg_data_type, step, step_desc, " +
                "prompt, thinking, content, tool_calls, created_at " +
                "FROM ai_agent_session_msg_step WHERE msg_id = ? ORDER BY step";
        return jdbcTemplate.query(sql, rowMapper, msgId);
    }

    /**
     * 根据消息 ID 和消息类型查询步骤列表，按 step ASC 排序
     * <p>
     * 用于历史消息加载时返回前端展示的步骤明细（msg_type=0）。
     */
    public List<AgentMsgStepEntity> findByMsgIdAndType(Long msgId, int msgType) {
        String sql = "SELECT id, msg_id, msg_type, msg_data_type, step, step_desc, " +
                "prompt, thinking, content, tool_calls, created_at " +
                "FROM ai_agent_session_msg_step " +
                "WHERE msg_id = ? AND msg_type = ? ORDER BY step ASC";
        return jdbcTemplate.query(sql, rowMapper, msgId, msgType);
    }

    /**
     * 根据消息 ID 和步骤编号查询该步骤的所有交互记录
     * <p>
     * 一个步骤可能包含多轮模型交互（如 tool call 自循环），
     * 每轮产生一条记录，按 id 正序返回以还原执行时序。
     */
    public List<AgentMsgStepEntity> findByMsgIdAndStep(Long msgId, int step) {
        String sql = "SELECT id, msg_id, msg_type, msg_data_type, step, step_desc, " +
                "prompt, thinking, content, tool_calls, created_at " +
                "FROM ai_agent_session_msg_step WHERE msg_id = ? AND step = ? ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper, msgId, step);
    }

    /**
     * 根据消息 ID 删除所有分段
     */
    public void deleteByMsgId(Long msgId) {
        jdbcTemplate.update("DELETE FROM ai_agent_session_msg_step WHERE msg_id = ?", msgId);
    }

    /**
     * 根据智能体 ID 删除所有分段（通过子查询关联消息表）
     */
    public void deleteByAgentId(Long agentId) {
        jdbcTemplate.update(
                "DELETE FROM ai_agent_session_msg_step WHERE msg_id IN " +
                        "(SELECT id FROM ai_agent_session_msg WHERE agent_id = ?)",
                agentId);
    }

    private final RowMapper<AgentMsgStepEntity> rowMapper = (rs, rowNum) -> {
        AgentMsgStepEntity e = new AgentMsgStepEntity();
        e.setId(rs.getLong("id"));
        e.setMsgId(rs.getLong("msg_id"));
        e.setMsgType(rs.getInt("msg_type"));
        e.setMsgDataType(rs.getInt("msg_data_type"));
        e.setStep(rs.getInt("step"));
        e.setStepDesc(rs.getString("step_desc"));
        e.setPrompt(rs.getString("prompt"));
        e.setThinking(rs.getString("thinking"));
        e.setContent(rs.getString("content"));
        e.setToolCalls(rs.getString("tool_calls"));
        e.setCreatedAt(rs.getLong("created_at"));
        return e;
    };
}
