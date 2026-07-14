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
 * message_created 事件 Payload — 消息已持久化，通知前端消息 ID 及模型信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageCreatedPayload {

    private Long msgId;
    private Long stepRecordId;
    private Integer stepNumber;
    private Long userMsgId;
    private Long assistantMsgId;
    /** 模型 Code（ID） */
    private String modelCode;
    /** 模型名称（显示用） */
    private String modelName;
}
