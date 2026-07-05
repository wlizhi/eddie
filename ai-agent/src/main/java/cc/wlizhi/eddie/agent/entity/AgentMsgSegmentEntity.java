/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息分段明细 — 映射 ai_agent_session_msg_segment 表
 * <p>
 * 记录 Agent 单次执行中每一步的完整内容（规划→工具调用→汇总→最终输出），
 * 按 msg_id + seq 排序可还原完整执行时序。
 */
@Setter
@Getter
public class AgentMsgSegmentEntity {

    private Long id;
    private Long msgId;
    private Integer seq;
    private String segType;     // planning / tool_call / tool_result / summarize / final
    private String content;     // 分段完整内容
    private Long createdAt;
}
