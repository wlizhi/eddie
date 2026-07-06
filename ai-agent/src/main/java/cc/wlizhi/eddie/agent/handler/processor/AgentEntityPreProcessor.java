/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.context.AgentContext;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentIteratorState;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import jakarta.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能体数据预处理器 — 填充 {@link AgentChatContext#agent}
 * <p>
 * 从 {@link AgentChatContext#session} 的 agentId 从 {@link AgentContext} 缓存中查询，
 * 验证智能体启用状态，将结果写入上下文。
 */
@Component
@Order(2)
public class AgentEntityPreProcessor implements AgentChatPreProcessor {

    @Resource
    private AgentContext agentContext;

    @Override
    public void process(AgentChatContext ctx) {
        Long agentId = ctx.getSession().getAgentId();

        AgentEntity agent = agentContext.getAgentById(agentId);
        if (agent == null) {
            throw new BadRequestException("agentId=" + agentId + " 不存在的智能体");
        }
        if (Objects.equals(agent.getEnabled(), 0)) {
            throw new BadRequestException(agent.getName() + " 已禁用，请启用后重试");
        }

        ctx.setAgent(agent);
        AgentIteratorState iteratorState = new AgentIteratorState();
        iteratorState.setMaxIterations(Math.clamp(agent.getMaxIterations(), 1, 1000));
        iteratorState.setCurrentIterator(new AtomicInteger(0));
        iteratorState.setAgentMode(AgentMode.CHAT);
        ctx.setIteratorState(iteratorState);
    }
}
