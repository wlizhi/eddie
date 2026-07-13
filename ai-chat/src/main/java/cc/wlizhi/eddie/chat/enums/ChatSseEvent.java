/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.chat.enums;

import lombok.Getter;

/**
 * Chat SSE 事件名称枚举 — 统一管理所有后端 → 前端的 SSE event name。
 * <p>
 * 枚举名使用 UPPER_UNDERSCORE，event name 使用 wire format 小写字符串，
 * 两者分离，避免枚举改名影响前后端协议。
 */
@Getter
public enum ChatSseEvent {

    // ==================== SSE 事件（后端 → 前端） ====================
    MESSAGE_CREATED("message_created"),
    THINKING("thinking"),
    ANSWER("answer"),
    TOOL_EXECUTION("tool_execution"),
    METADATA("metadata"),
    CANCELLED("cancelled"),
    ERROR("error"),

    // ==================== 内部事件（非 SSE，EventRegistry 跨请求通信用） ====================
    STOP("STOP"),
    ;

    /** SSE wire format 事件名 */
    private final String eventName;

    ChatSseEvent(String eventName) {
        this.eventName = eventName;
    }
}
