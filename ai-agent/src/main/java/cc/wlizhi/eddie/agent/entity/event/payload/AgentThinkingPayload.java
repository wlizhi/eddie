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
 * thinking 事件 Payload — 模型思考内容（流式）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThinkingPayload {

    private Long msgId;
    private Long stepId;
    private Integer step;
    private String text;
}
