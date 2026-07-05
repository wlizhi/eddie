/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.dao;

import cc.wlizhi.eddie.agent.entity.AgentMsgSegmentEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息分段明细 DAO — 操作 ai_agent_session_msg_segment 表（eddie-agent.db）
 */
@Repository
public class AgentMsgSegmentDao {

    @Resource(name = "agentJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public void insert(AgentMsgSegmentEntity entity) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO ai_agent_session_msg_segment (msg_id, seq, seg_type, content, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                entity.getMsgId(), entity.getSeq(), entity.getSegType(), entity.getContent(), now);
    }

    public List<AgentMsgSegmentEntity> findByMsgId(Long msgId) {
        String sql = "SELECT id, msg_id, seq, seg_type, content, created_at " +
                "FROM ai_agent_session_msg_segment WHERE msg_id = ? ORDER BY seq";
        return jdbcTemplate.query(sql, rowMapper, msgId);
    }

    /**
     * 根据消息 ID 删除所有分段
     */
    public void deleteByMsgId(Long msgId) {
        jdbcTemplate.update("DELETE FROM ai_agent_session_msg_segment WHERE msg_id = ?", msgId);
    }

    /**
     * 根据智能体 ID 删除所有分段（通过子查询关联消息表）
     */
    public void deleteByAgentId(Long agentId) {
        jdbcTemplate.update(
                "DELETE FROM ai_agent_session_msg_segment WHERE msg_id IN " +
                        "(SELECT id FROM ai_agent_session_msg WHERE agent_id = ?)",
                agentId);
    }

    private final RowMapper<AgentMsgSegmentEntity> rowMapper = (rs, rowNum) -> {
        AgentMsgSegmentEntity e = new AgentMsgSegmentEntity();
        e.setId(rs.getLong("id"));
        e.setMsgId(rs.getLong("msg_id"));
        e.setSeq(rs.getInt("seq"));
        e.setSegType(rs.getString("seg_type"));
        e.setContent(rs.getString("content"));
        e.setCreatedAt(rs.getLong("created_at"));
        return e;
    };
}
