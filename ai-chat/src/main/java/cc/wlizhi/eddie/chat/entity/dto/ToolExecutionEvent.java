/**
 * ToolExecutionEvent — 工具执行事件 DTO
 * <p>
 * 用于在 SSE 流中向前端推送工具执行状态，包含开始和完成两个阶段。
 * 所有数据均来自本地，不消耗模型 token。
 */
package cc.wlizhi.eddie.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToolExecutionEvent {

    /**
     * 事件状态：start / complete
     */
    private String status;

    /**
     * 工具名称（如 built_in_search）
     */
    private String toolName;

    /**
     * 工具调用参数（JSON 字符串）
     */
    private String arguments;

    /**
     * 工具执行结果（status=complete 时有值）
     */
    private String result;

    /**
     * 是否执行出错
     */
    private boolean error;

    public static ToolExecutionEvent start(String toolName, String arguments) {
        ToolExecutionEvent event = new ToolExecutionEvent();
        event.setStatus("start");
        event.setToolName(toolName);
        event.setArguments(arguments);
        return event;
    }

    public static ToolExecutionEvent complete(String toolName, String result, boolean error) {
        ToolExecutionEvent event = new ToolExecutionEvent();
        event.setStatus("complete");
        event.setToolName(toolName);
        event.setResult(result);
        event.setError(error);
        return event;
    }
}
