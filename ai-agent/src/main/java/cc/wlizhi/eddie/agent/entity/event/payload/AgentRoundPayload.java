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
 * 每一轮循环的开始与结束事件
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentRoundPayload {

    private Long msgId;
    private Long stepId;
    private Integer step;
    private int round;
}
