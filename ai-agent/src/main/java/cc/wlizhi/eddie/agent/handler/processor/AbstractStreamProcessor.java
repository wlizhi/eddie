/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessor;
import cc.wlizhi.eddie.agent.util.TokenStatsHelper;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 流式响应处理器 — 抽象模板方法基类
 * <p>
 * 定义流式处理的公共骨架：
 * <ol>
 *     <li>{@link #beforeStream(AgentChatContext)} — 流开始前钩子</li>
 *     <li>遍历每条 {@link ChatResponse}，依次执行：</li>
 *     <ul>
 *         <li>{@link #handleThinking(AgentChatContext, ChatResponse)} — 提取思考内容</li>
 *         <li>{@link #handleAnswer(AgentChatContext, ChatResponse)} — 提取回答内容</li>
 *         <li>{@link #handleCustomEvent(AgentChatContext, ChatResponse)} — 模式特有事件</li>
 *     </ul>
 *     <li>{@link #afterStream(AgentChatContext, ChatResponse)} — 流结束后钩子（携带最后一帧）</li>
 * </ol>
 * 子类只需覆盖需要自定义的钩子方法，公共逻辑由基类统一提供。
 * <p>
 * 适用于需要实时推送打字机效果的场景（thinking / answer 逐块推送）。
 * 若需要阻塞式等待全量结果，请使用 {@link AbstractBlockingProcessor}。
 */
public abstract class AbstractStreamProcessor implements ResponseStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamProcessor.class);

    @Resource
    protected AgentEventPublisher publisher;

    @Resource
    protected AgentMsgDao agentMsgDao;
    @Resource
    private EventRegistry eventRegistry;

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        // 钩子：流开始前的预处理
        beforeStream(ctx);

        // 使用 Iterator 遍历流，自然捕获最后一个 chunk 供后续 token 提取使用
        Iterator<ChatResponse> iterator = requestSpec.stream().chatResponse()
                .takeWhile(_ -> !checkUserStopEvent(ctx))
                .toStream()
                .iterator();

        ChatResponse lastResponse = null;
        try {
            while (iterator.hasNext()) {
                ChatResponse res = iterator.next();
                lastResponse = res;

                // 1. 提取并推送思考内容（JSON 格式 via AgentEventPublisher）
                handleThinking(ctx, res);

                // 2. 提取并推送回答内容（JSON 格式 via AgentEventPublisher）
                handleAnswer(ctx, res);

                // 3. 模式特有事件（子类可覆盖）
                handleCustomEvent(ctx, res);
            }
        } catch (Exception ex) {
            if (ex.getCause() instanceof InterruptedException) {
                onStreamInterrupted(ctx);
            }
            throw ex;
        }


        // 统一收尾：从最后一帧提取 token 统计 → 推送 → 持久化
        if (lastResponse != null) {
            Usage usage = lastResponse.getMetadata().getUsage();
            log.debug("afterStream，getTotalTokens:{}，getPromptTokens：{}，getCompletionTokens：{}", usage.getTotalTokens(), usage.getPromptTokens(), usage.getCompletionTokens());
            TokenStatsHelper.extractAndMergeTokenStats(ctx, lastResponse);
            if (ctx.getOutput().getTokenStatists() != null) {
                publisher.metadata(ctx, ctx.getOutput().getTokenStatists());
            }
            TokenStatsHelper.persistTokenStats(ctx, agentMsgDao);
        }

        // 钩子：流结束后的收尾（携带最后一帧，子类按需使用）
        afterStream(ctx, lastResponse);
    }

    private boolean checkUserStopEvent(AgentChatContext ctx) {
        if (ctx.getEvent().getAgentThread().isInterrupted()) {
            return true;
        }
        // 用户中断指令发出，应当中断。
        String stopKey = EventRegistry.key(AgentEvent.STOP_MSG.name().toLowerCase(), ctx.getAgentMsg().getId().toString());
        if (Objects.equals(eventRegistry.get(stopKey), AgentEvent.STOP_MSG.name().toLowerCase())) {
            log.info("用户点击停止回答");
            return true;
        }
        return false;
    }

    // ================ 公共方法（子类通常无需覆盖） ================

    /**
     * 从 ChatResponse 提取模型思考内容（reasoning_content）
     * 兼容 OpenAI / DeepSeek 等不同服务商的 metadata key
     */
    protected void handleThinking(AgentChatContext ctx, ChatResponse response) {
        try {
            var metadata = Objects.requireNonNull(response.getResult()).getOutput().getMetadata();
            // key "reasoningContent" 兼容 OpenAI reasoning API
            Object reasoning = metadata.get("reasoningContent");
            if (reasoning == null) {
                // fallback: DeepSeek 使用的 key
                reasoning = metadata.get("reasoning_content");
            }
            if (reasoning != null && !reasoning.toString().isEmpty()) {
                String text = reasoning.toString();
                publisher.thinking(ctx, null, text);
                ctx.getOutput().getFullThinking().append(text);
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    /**
     * 从 ChatResponse 提取模型生成的文本内容
     */
    protected void handleAnswer(AgentChatContext ctx, ChatResponse response) {
        try {
            String answer = response.getResults().stream()
                    .map(Generation::getOutput)
                    .map(AbstractMessage::getText).filter(Objects::nonNull)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.joining());
            if (!answer.isEmpty()) {
                publisher.answer(ctx, null, answer);
                ctx.getOutput().getFullAnswer().append(answer);
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    // ================ 钩子方法（子类可按需覆盖） ================

    /**
     * 流开始前的预处理
     * <p>
     * 子类可在此向 sink 推送前置事件（如 "mode_switch"、"plan_start" 等）
     */
    protected void beforeStream(AgentChatContext ctx) {
        // 默认空实现
    }

    /**
     * 模式特有事件处理
     * <p>
     * 子类可在此推送该模式专属的 SSE 事件（如 "tool_call"、"execute_step" 等）
     */
    protected void handleCustomEvent(AgentChatContext ctx, ChatResponse response) {
        // 默认空实现
    }

    /**
     * 流处理过程中线程中断后的业务处理钩子。
     * <p>
     * 触发场景：前端刷新或离开页面 → SSE 连接断开 → 虚拟线程被中断
     * → 流式 API 抛出 {@link InterruptedException}。
     * <p>
     * 此时前端已断开，无需推送 SSE 事件，只需做好数据持久化，
     * 确保刷新后页面能正确展示中断状态。
     * <p>
     * 默认空实现，子类按需覆写。
     *
     * @param ctx 流处理上下文
     */
    protected void onStreamInterrupted(AgentChatContext ctx) {
        // 默认空实现
    }

    /**
     * 流结束后的收尾钩子。
     * <p>
     * 基类已在 {@link #process} 中完成 token 统计提取 + 推送 + 持久化，
     * 子类只需在此处理模式特有的收尾逻辑（如内容持久化、步骤记录更新等）。
     *
     * @param ctx          流处理上下文
     * @param lastResponse 流式响应的最后一帧（可为 null），子类可按需使用
     */
    protected void afterStream(AgentChatContext ctx, ChatResponse lastResponse) {
        // 默认空实现
    }
}
