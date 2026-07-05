/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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

    public List<AgentMsgStepEntity> findByMsgId(Long msgId) {
        String sql = "SELECT id, msg_id, msg_type, msg_data_type, step, step_desc, " +
                "prompt, thinking, content, tool_calls, created_at " +
                "FROM ai_agent_session_msg_step WHERE msg_id = ? ORDER BY step";
        return jdbcTemplate.query(sql, rowMapper, msgId);
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
