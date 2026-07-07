package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class AgentIteratorState {
    /**
     * 当前模式
     */
    private volatile AgentMode agentMode;
    /**
     * 最大迭代次数
     */
    private Integer maxIterations;
    /**
     * 当前迭代次数，从0开始
     */
    private AtomicInteger currentIterator;
}
