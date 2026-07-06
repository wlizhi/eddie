/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 阻塞式响应处理器 — 抽象模板方法基类
 * <p>
 * 定义阻塞式（全量等待）处理的公共骨架：
 * <ol>
 *     <li>{@link #beforeBlock(AgentChatContext)} — 阻塞调用前钩子</li>
 *     <li>{@link #doBlock(AgentChatContext, ChatClient.ChatClientRequestSpec)} — 执行阻塞调用并返回全量结果</li>
 *     <li>{@link #afterBlock(AgentChatContext)} — 阻塞调用后钩子</li>
 * </ol>
 * <p>
 * 适用于需要模型返回结构化内容（如 JSON mode）、
 * 或不需要打字机效果、只需一次性获取全量结果的场景。
 * <p>
 * 若需要实时推送打字机效果，请使用 {@link AbstractStreamProcessor}。
 */
public abstract class AbstractBlockingProcessor implements ResponseStreamProcessor {

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        // 钩子：阻塞调用前的预处理
        beforeBlock(ctx);

        // 执行阻塞调用，等待全量结果
        ChatResponse response = doBlock(ctx, requestSpec);
        ctx.setLastResponse(response);

        // 钩子：阻塞调用后的收尾
        afterBlock(ctx);
    }

    // ================ 抽象方法（子类必须实现） ================

    /**
     * 执行阻塞式调用并返回全量 {@link ChatResponse}
     * <p>
     * 子类在此方法中调用 {@code requestSpec.call()} 或 {@code requestSpec.call(Class)}，
     * 并在返回前通过 {@code ctx.getSink().next(...)} 向前端推送自定义 SSE 事件。
     *
     * @param ctx         当前请求上下文
     * @param requestSpec 已构建好的 ChatClient 请求体
     * @return 全量 ChatResponse
     */
    protected abstract ChatResponse doBlock(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec);

    // ================ 钩子方法（子类可按需覆盖） ================

    /**
     * 阻塞调用前的预处理
     * <p>
     * 子类可在此向 sink 推送前置事件（如 "structured_start" 等）
     */
    protected void beforeBlock(AgentChatContext ctx) {
        // 默认空实现
    }

    /**
     * 阻塞调用后的收尾
     * <p>
     * 子类可在此推送结束事件（如 "structured_complete"）或执行资源清理
     */
    protected void afterBlock(AgentChatContext ctx) {
        // 默认空实现
    }
}
