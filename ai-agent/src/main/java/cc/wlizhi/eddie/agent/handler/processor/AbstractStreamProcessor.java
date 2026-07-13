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
 *     <li>{@link #afterStream(AgentChatContext)} — 流结束后钩子</li>
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

        // 日志计数器：记录 chunk 数量，用于诊断工具调用后是否卡死
        AtomicInteger chunkCount = new AtomicInteger(0);
        long streamStart = System.currentTimeMillis();

        requestSpec.stream().chatResponse()
                .takeWhile(_ -> !checkUserStopEvent(ctx)).toStream()
                .forEach(res -> {
                   chunkCount.incrementAndGet();
                    // 保存最后一次响应，供后续提取 tool_calls / token 用量
                    ctx.setLastResponse(res);

                    // 1. 提取并推送思考内容（JSON 格式 via AgentEventPublisher）
                    handleThinking(ctx, res);

                    // 2. 提取并推送回答内容（JSON 格式 via AgentEventPublisher）
                    handleAnswer(ctx, res);

                    // 3. 模式特有事件（子类可覆盖）
                    handleCustomEvent(ctx, res);
                });

        long elapsed = System.currentTimeMillis() - streamStart;
        log.debug("[StreamDiagnostic] 流处理完成, 共 {} chunks, 耗时 {}ms", chunkCount.get(), elapsed);

        // 钩子：流结束后的收尾
        afterStream(ctx);
    }

    private boolean checkUserStopEvent(AgentChatContext ctx) {
        if (ctx.getAgentThread().isInterrupted()) {
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

    protected boolean breakInStreamIfNecessary(AgentChatContext ctx) {
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
                ctx.getFullThinking().append(text);
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
                ctx.getFullAnswer().append(answer);
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
     * 流结束后的收尾 — 通用逻辑：提取 token 统计 + 增量持久化 + 发射 metadata 事件
     * <p>
     * 子类若覆盖此方法，<b>必须</b>调用 {@code super.afterStream(ctx)} 以确保统计逻辑执行。
     */
    protected void afterStream(AgentChatContext ctx) {
        Usage usage = ctx.getLastResponse().getMetadata().getUsage();
        log.debug("afterStream，getTotalTokens:{}，getPromptTokens：{}，getCompletionTokens：{}", usage.getTotalTokens(), usage.getPromptTokens(), usage.getCompletionTokens());
        // 1. 从最后一条 ChatResponse 提取 token 统计，增量合并到 ctx.tokenStatists
        TokenStatsHelper.extractAndMergeTokenStats(ctx);

        // 2. 先推流给前端（低延迟，用户先看到数据）
        if (ctx.getTokenStatists() != null) {
            publisher.metadata(ctx, ctx.getTokenStatists());
        }

        // 3. 后持久化到数据库（I/O 操作，不阻塞用户体验）
        TokenStatsHelper.persistTokenStats(ctx, agentMsgDao);
    }
}
