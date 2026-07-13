/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 消息步骤明细响应 VO — 对应 ai_agent_session_msg_step 表，返回给前端展示。
 * <p>
 * 与实体 AgentMsgStepEntity 字段一一对应，但 toolCalls 已解析为结构化列表，
 * 避免前端需要手动 JSON.parse()。
 */
@Getter
@Setter
public class AgentMsgStepVO {

    private Long id;
    private Long msgId;
    private Integer msgType;
    private Integer msgDataType;
    private Integer stepNumber;
    private String stepDesc;
    private String prompt;
    private String thinking;
    private String content;

    /**
     * 工具调用列表（已解析为结构化数据，null/空字符串时返回空列表）
     */
    private List<AgentToolCallVO> toolCalls;

    private Long createdAt;
}
