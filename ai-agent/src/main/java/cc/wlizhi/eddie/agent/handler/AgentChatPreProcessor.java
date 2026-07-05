/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;

/**
 * Agent 聊天数据预处理器 — 策略接口
 * <p>
 * 每个实现类负责填充 {@link AgentChatContext} 中的一个或一组字段，
 * 通过 {@link org.springframework.core.annotation.Order @Order} 控制执行顺序。
 * 使用方注入 {@code List<AgentChatPreProcessor>} 遍历调用。
 * <p>
 * 参考 {@code cc.wlizhi.eddie.chat.handler.ChatPreProcessor} 的责任链模式。
 */
@FunctionalInterface
public interface AgentChatPreProcessor {

    /**
     * 执行预处理，将结果写入 ctx
     *
     * @param ctx 当前请求上下文，上游处理器的结果可在下游读取
     */
    void process(AgentChatContext ctx);
}
