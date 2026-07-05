/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.preprocessor;

import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import jakarta.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 会话数据预处理器 — 填充 {@link AgentChatContext#session}
 * <p>
 * 从 {@link AgentChatRequest#getSessionId()} 查询会话记录，
 * 验证会话存在性，将结果写入上下文。
 */
@Component
@Order(1)
public class AgentSessionPreProcessor implements AgentChatPreProcessor {

    @Resource
    private AgentSessionDao agentSessionDao;

    @Override
    public void process(AgentChatContext ctx) {
        AgentChatRequest request = ctx.getOriginalRequest();
        Long sessionId = request.getSessionId();

        AgentSessionEntity session = agentSessionDao.findById(sessionId);
        if (session == null) {
            throw new BadRequestException("sessionId=" + sessionId + " 不存在的会话");
        }

        ctx.setSession(session);
    }
}
