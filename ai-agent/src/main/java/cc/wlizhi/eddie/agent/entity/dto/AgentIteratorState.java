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

    /**
     * 是否已完成（由各 Processor 在适当时候设置）。
     * <p>
     * 与 {@link #agentMode} 语义正交——agentMode 表示"以何种策略处理"，
     * finished 表示"处理是否已终结"。设置后 {@code shouldBreakIterator()} 将返回 true。
     */
    private volatile boolean finished;
}
