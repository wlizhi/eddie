/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.chat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 助手聊天工具执行事件 Payload
 * <p>
 * 对应 SSE event: tool_execution，包裹在 ApiResult 信封中。
 * 与 Agent 的 {@code ToolExecutionPayload} 结构一致，但不含 stepId/step 字段。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatToolExecPayload {

    /** 消息 ID（用于审批场景） */
    private Long msgId;
    /** 工具名称 */
    private String toolName;
    /** 事件状态：start / complete / pending_approval */
    private String status;
    /** 工具调用参数（JSON 字符串） */
    private String arguments;
    /** 工具执行结果 */
    private Object result;
    /** 是否执行出错 */
    private boolean error;
    /** 工具调用序号（用于审批 key 唯一标识） */
    private int seq;
}
