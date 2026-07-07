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
 * cancelled 事件 Payload — 任务取消
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelledPayload {

    private String eventType;
    private Long msgId;
    private Long stepId;
    private Integer step;
    private String reason;
}
