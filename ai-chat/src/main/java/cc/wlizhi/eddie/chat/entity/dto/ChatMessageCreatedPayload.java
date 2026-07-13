/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.chat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * message_created 事件 Payload — 消息已持久化，通知前端消息 ID
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreatedPayload {

    /** 用户消息 ID */
    private Long userMsgId;
    /** 助手消息 ID（占位消息） */
    private Long assistantMsgId;
}
