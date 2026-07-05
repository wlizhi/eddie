/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.preprocessor;

import cc.wlizhi.eddie.agent.context.AgentContext;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import jakarta.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    }
}
