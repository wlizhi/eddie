/**
 * ToolExecutionEvent — 工具执行事件 DTO
 * <p>
 * 用于在 SSE 流中向前端推送工具执行状态，包含开始和完成两个阶段。
 * 所有数据均来自本地，不消耗模型 token。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.chat.entity.dto;

import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatToolExecutionEvent {

    /**
     * 事件状态：START / COMPLETE
     */
    private ToolExecutionStatus status;

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

    /**
     * 工具调用序号（用于审批 key 唯一标识，由外层包装器设置）
     */
    private int seq;

    public static ChatToolExecutionEvent start(String toolName, String arguments) {
        ChatToolExecutionEvent event = new ChatToolExecutionEvent();
        event.setStatus(ToolExecutionStatus.START);
        event.setToolName(toolName);
        event.setArguments(arguments);
        return event;
    }

    /**
     * 创建 complete 事件，携带 arguments 以保留工具调用参数（用于持久化）
     */
    public static ChatToolExecutionEvent complete(String toolName, String arguments, String result, boolean error) {
        ChatToolExecutionEvent event = new ChatToolExecutionEvent();
        event.setStatus(ToolExecutionStatus.COMPLETE);
        event.setToolName(toolName);
        event.setArguments(arguments);
        event.setResult(result);
        event.setError(error);
        return event;
    }

    /**
     * 创建 rejected 事件，标记工具调用被用户拒绝
     */
    public static ChatToolExecutionEvent rejected(String toolName, String arguments) {
        ChatToolExecutionEvent event = new ChatToolExecutionEvent();
        event.setStatus(ToolExecutionStatus.REJECTED);
        event.setToolName(toolName);
        event.setArguments(arguments);
        event.setResult("用户拒绝了此工具调用");
        event.setError(true);
        return event;
    }
}
