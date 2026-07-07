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
 * message_created 事件 Payload — 消息已持久化，通知前端消息 ID
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreatedPayload {

    private String eventType;
    private Long msgId;
    private Long stepId;
    private Integer step;
    private Long userMsgId;
    private Long assistantMsgId;
}
