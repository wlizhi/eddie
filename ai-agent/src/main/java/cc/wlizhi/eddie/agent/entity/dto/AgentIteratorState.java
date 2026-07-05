package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentIteratorState {
    private AgentMode agentMode;
    private Integer maxIterations;
    private Integer currentIterator;

}
