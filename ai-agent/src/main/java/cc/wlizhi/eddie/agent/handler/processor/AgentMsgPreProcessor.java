/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 消息持久化预处理器 — 填充 {@link AgentChatContext#userMsg} 和 {@link AgentChatContext#agentMsg}
 * <p>
 * 职责：
 * <ol>
 *   <li>（事务内）保存用户消息（role=user, status=COMPLETED）到数据库</li>
 *   <li>（事务内）创建 AI 回复占位消息（role=assistant, status=PROCESSING）到数据库</li>
 *   <li>（事务外）更新会话消息计数，允许失败</li>
 * </ol>
 * <p>
 * 用户消息与助手占位消息在同一个事务内写入，保证原子性；
 * 会话元数据更新独立于事务外，极端情况失败不影响消息已持久化。
 * <p>
 * 执行顺序：在所有数据查询 PreProcessor 之后（Order 5），
 * 确保 {@link AgentChatContext#session}、{@link AgentChatContext#agent}、
 * {@link AgentChatContext#modelProvider}、{@link AgentChatContext#useModelInfo} 均已就绪。
 */
@Component
@Order(5)
public class AgentMsgPreProcessor implements AgentChatPreProcessor {

    private static final Logger log = LoggerFactory.getLogger(AgentMsgPreProcessor.class);

    @Resource
    private AgentMsgDao agentMsgDao;

    @Resource
    private AgentSessionDao agentSessionDao;

    @Resource(name = "agentTransactionTemplate")
    private TransactionTemplate agentTransactionTemplate;

    @Override
    public void process(AgentChatContext ctx) {
        AgentChatRequest request = ctx.getOriginalRequest();
        AgentEntity agent = ctx.getAgent();
        AgentSessionEntity session = ctx.getSession();

        // ==================== 事务内：保存用户消息 + 创建 AI 占位 ====================
        AgentMsgEntity userMsg = new AgentMsgEntity();
        AgentMsgEntity agentMsg = new AgentMsgEntity();

        agentTransactionTemplate.executeWithoutResult(status -> {
            // ① 用户消息（round_seq=0，插入后回填为自己的 ID）
            userMsg.setSessionId(session.getId());
            userMsg.setAgentId(agent.getId());
            userMsg.setRole("user");
            userMsg.setContent(request.getMessage());
            userMsg.setMsgStatus("COMPLETED");
            userMsg.setModelCode(ctx.getUseModelInfo().getId());
            userMsg.setModelName(ctx.getUseModelInfo().getName());
            userMsg.setRoundSeq(0L);

            log.info("保存用户消息, sessionId={}, agentId={}, content={}",
                    session.getId(), agent.getId(), truncate(request.getMessage()));
            agentMsgDao.insert(userMsg);
            userMsg.setId(agentMsgDao.findLastInsertId());

            // ② AI 占位消息（round_seq=0，流结束后统一回填 user+assistant）
            agentMsg.setSessionId(session.getId());
            agentMsg.setAgentId(agent.getId());
            agentMsg.setRole("assistant");
            agentMsg.setContent("");
            if (ctx.getModelProvider() != null) {
                agentMsg.setProviderId(ctx.getModelProvider().getId());
            }
            if (ctx.getUseModelInfo() != null) {
                agentMsg.setModelCode(ctx.getUseModelInfo().getId());
                agentMsg.setModelName(ctx.getUseModelInfo().getName());
            }
            agentMsg.setMsgStatus("PROCESSING");
            agentMsg.setRoundSeq(0L);

            log.info("创建 AI 回复占位消息, sessionId={}, agentId={}, msgStatus=PROCESSING",
                    session.getId(), agent.getId());
            agentMsgDao.insert(agentMsg);
            agentMsg.setId(agentMsgDao.findLastInsertId());
        });

        ctx.setUserMsg(userMsg);
        ctx.setAgentMsg(agentMsg);

        // ==================== 事务外：更新会话元数据（允许失败） ====================
        try {
            agentSessionDao.touchAndIncrementMessageCount(session.getId(), 2, 0);
        } catch (Exception e) {
            log.warn("更新会话消息计数失败，不影响消息已持久化, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 截断消息内容至前 50 字符用于日志
     */
    private static String truncate(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.length() > 50 ? msg.substring(0, 50) + "..." : msg;
    }
}
