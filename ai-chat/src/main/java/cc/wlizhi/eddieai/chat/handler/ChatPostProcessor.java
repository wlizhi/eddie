package cc.wlizhi.eddieai.chat.handler;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;

/**
 * 聊天后置处理器（SSE 流结束后执行）
 * <p>
 * 职责：消息持久化、会话时间更新等，在流结束后异步执行，不阻塞主流程。
 */
public interface ChatPostProcessor {

    /**
     * 后置处理
     *
     * @param ctx 聊天上下文（含完整 thinking、answer、token 统计等）
     */
    void process(ChatContext ctx);
}
