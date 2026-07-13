/**
 * @author Eddie
 * {@code @date} 2026-07-07
 */

package cc.wlizhi.eddie.agent.entity.event.payload;

import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * metadata 事件 Payload — 每一轮执行完毕的元数据（token 用量、耗时等）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetadataPayload {

    private Long msgId;
    private Long stepId;
    private Integer step;
    private AgentTokenStatists stats;
}
