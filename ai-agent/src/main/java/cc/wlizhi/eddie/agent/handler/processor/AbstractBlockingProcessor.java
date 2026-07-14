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
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.Objects;
import java.util.stream.Collectors;

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
 * 基类在 {@code afterBlock()} 之后自动执行以下统一收尾（子类无需关心）：
 * <ol>
 *     <li>从全量 {@link ChatResponse} 提取 thinking + content，追加到累计上下文</li>
 *     <li>提取 token 统计并合并到 {@code ctx.tokenStatists}</li>
 *     <li>发射 metadata SSE 事件给前端（先推流）</li>
 *     <li>增量持久化 token 统计到数据库（后落库）</li>
 * </ol>
 * <p>
 * 适用于需要模型返回结构化内容（如 JSON mode）、
 * 或不需要打字机效果、只需一次性获取全量结果的场景。
 * <p>
 * 若需要实时推送打字机效果，请使用 {@link AbstractStreamProcessor}。
 */
public abstract class AbstractBlockingProcessor implements ResponseStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(AbstractBlockingProcessor.class);

    @Resource
    protected AgentEventPublisher publisher;

    @Resource
    protected AgentMsgDao agentMsgDao;

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        // 钩子：阻塞调用前的预处理
        beforeBlock(ctx);

        // 执行阻塞调用，等待全量结果
        ChatResponse response = doBlock(ctx, requestSpec);
        if (response == null) {
            throw new AppException(ApiResultCode.PROVIDER_CALL_FAILED,
                    "阻塞式模型调用返回 null，请检查日志确认异常原因");
        }

        // ==================== 基类统一收尾（子类不可跳过） ====================

        // 1. 从全量 response 提取 thinking + content，追加到累计上下文
        extractBlockingContent(ctx, response);

        Usage usage = response.getMetadata().getUsage();
        log.debug("doBlock，getTotalTokens:{}，getPromptTokens：{}，getCompletionTokens：{}", usage.getTotalTokens(), usage.getPromptTokens(), usage.getCompletionTokens());

        // 2. 从 ChatResponse 提取 token 统计，增量合并到 ctx.tokenStatists
        TokenStatsHelper.extractAndMergeTokenStats(ctx, response);

        // 3. 先推流给前端（低延迟，用户先看到数据）
        if (ctx.getOutput().getTokenStatists() != null) {
            publisher.metadata(ctx, ctx.getOutput().getTokenStatists());
        }

        // 4. 后持久化到数据库（I/O 操作，不阻塞用户体验）
        TokenStatsHelper.persistTokenStats(ctx, agentMsgDao);

        // 钩子：阻塞调用后的自定义收尾
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

    // ================ 公共方法（子类通常无需覆盖） ================

    /**
     * 从全量阻塞式 {@link ChatResponse} 提取 thinking + content，追加到累计上下文。
     * <p>
     * 流式场景由 {@link AbstractStreamProcessor} 逐 chunk 累加，
     * 阻塞式场景由本方法一次性提取全量内容。
     */
    protected void extractBlockingContent(AgentChatContext ctx, ChatResponse response) {
        try {
            // 提取 thinking（reasoning_content）
            var metadata = Objects.requireNonNull(response.getResult()).getOutput().getMetadata();
            Object reasoning = metadata.get("reasoningContent");
            if (reasoning == null) {
                reasoning = metadata.get("reasoning_content");
            }
            if (reasoning != null && !reasoning.toString().isEmpty()) {
                String text = reasoning.toString();
                ctx.getOutput().getFullThinking().append(text);
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }

        try {
            // 提取 answer（content）
            String answer = response.getResults().stream()
                    .map(Generation::getOutput)
                    .map(AbstractMessage::getText).filter(Objects::nonNull)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.joining());
            if (!answer.isEmpty()) {
                ctx.getOutput().getFullAnswer().append(answer);
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }
}
