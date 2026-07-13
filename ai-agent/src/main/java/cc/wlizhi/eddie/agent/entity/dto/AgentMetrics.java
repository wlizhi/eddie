/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 指标/计数上下文 — 存放请求时间、步骤计数等统计信息。
 *
 * @author Eddie
 * {@code @date} 2026-07-13
 */
@Getter
@Setter
public class AgentMetrics {

    /**
     * 请求开始时间戳
     */
    private long startTime;

    /**
     * 当前步骤编号（1-based），表示当前正在执行的任务规划步骤
     */
    private Integer currentStepNumber;
}
