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
 * answer 事件 Payload — 模型回答内容（流式）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentAnswerPayload {

    private Long msgId;
    private Long stepRecordId;
    private Integer stepNumber;
    private String text;
}
