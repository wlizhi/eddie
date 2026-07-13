/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 工具调用记录响应 VO — 对应 ai_agent_session_msg / ai_agent_session_msg_step 中 tool_calls 的解析结果。
 * <p>
 * 数据库存储的是 ChatToolExecutionEvent[] JSON 字符串，返回给前端时解析为该 VO 列表，
 * 仅暴露前端需要的字段，隐藏 status 等内部运行时状态。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentToolCallVO {

    /**
     * 工具调用序号（用于前端标识工具调用顺序）
     */
    private Integer seq;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具调用参数（JSON 字符串）
     */
    private String arguments;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 是否执行出错
     */
    private Boolean error;
}
