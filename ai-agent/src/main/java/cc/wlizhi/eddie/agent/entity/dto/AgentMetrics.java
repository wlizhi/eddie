/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.entity.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StopWatch;

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

    /**
     * 全链路耗时统计，各策略实现类通过 ctx 记录各阶段耗时，finally 中统一打印
     */
    private final StopWatch stopWatch = new StopWatch("agent-chat");
}
