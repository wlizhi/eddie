/**
 * @author Eddie
 * {@code @date} 2026-07-07
 */

package cc.wlizhi.eddie.agent.entity.event.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * tool_execution 事件 Payload — 工具执行状态与结果
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionPayload {

    private Long msgId;
    private Long stepId;
    private Integer step;
    private String toolName;
    private String status;
    private String arguments;
    private Object result;
    private boolean error;
}
