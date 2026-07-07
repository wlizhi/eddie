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
 * round_start 事件 Payload — 新一轮迭代开始
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundStartPayload {

    private String eventType;
    private Long msgId;
    private Long stepId;
    private Integer step;
    private int round;
}
