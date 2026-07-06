package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class AgentIteratorState {
    private volatile AgentMode agentMode;
    private Integer maxIterations;
    private AtomicInteger currentIterator;
}
