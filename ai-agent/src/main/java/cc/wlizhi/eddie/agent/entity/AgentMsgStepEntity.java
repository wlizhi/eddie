/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息分段明细 — 映射 ai_agent_session_msg_step 表
 * <p>
 * 记录 Agent 单次执行中每一步的完整内容（规划→工具调用→汇总→最终输出），
 * 按 msg_id + step 排序可还原完整执行时序。
 */
@Setter
@Getter
public class AgentMsgStepEntity {

    private Long id;
    private Long msgId;
    private Integer msgType;        // 消息类型，0 前端展示，1 后端任务规划
    private Integer msgDataType;    // 消息数据类型，0 文本，1 json字符串
    private Integer step;           // 阶段，后端流程编排中的任务阶段
    private String stepDesc;        // 阶段描述信息
    private String prompt;          // 提示词
    private String thinking;        // 思考内容
    private String content;         // 分段完整内容
    private String toolCalls;       // 工具调用 JSON
    private Long createdAt;
}
