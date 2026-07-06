/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessor;
import cc.wlizhi.eddie.common.util.PriceCalculator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
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

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        // 钩子：流开始前的预处理
        beforeStream(ctx);

        // 日志计数器：记录 chunk 数量，用于诊断工具调用后是否卡死
        AtomicInteger chunkCount = new AtomicInteger(0);
        long streamStart = System.currentTimeMillis();

        // 遍历响应流 — 使用 10 分钟超时防止 Flux 无限阻塞
        try {
            requestSpec.stream().chatResponse()
                    .toStream()
                    .forEach(res -> {
                        int idx = chunkCount.incrementAndGet();
                        // 保存最后一次响应，供后续提取 tool_calls / token 用量
                        ctx.setLastResponse(res);

                        // 检测是否有 tool_calls（用于日志诊断）
                        boolean hasToolCall = res.getResults().stream()
                                .anyMatch(g -> g.getOutput() != null
                                        && g.getOutput().getToolCalls() != null
                                        && !g.getOutput().getToolCalls().isEmpty());
                        if (hasToolCall) {
                            String finishReason = res.getResult() != null
                                    && res.getResult().getMetadata() != null
                                    ? res.getResult().getMetadata().getFinishReason()
                                    : "N/A";
                            log.debug("[StreamDiagnostic] chunk#{} 包含 tool_calls, finishReason={}",
                                    idx, finishReason);
                        }

                        // 1. 提取并推送思考内容（JSON 格式 via AgentEventPublisher）
                        handleThinking(ctx, res);

                        // 2. 提取并推送回答内容（JSON 格式 via AgentEventPublisher）
                        handleAnswer(ctx, res);

                        // 3. 模式特有事件（子类可覆盖）
                        handleCustomEvent(ctx, res);
                    });

            long elapsed = System.currentTimeMillis() - streamStart;
            log.debug("[StreamDiagnostic] 流处理完成, 共 {} chunks, 耗时 {}ms", chunkCount.get(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - streamStart;
            log.warn("[StreamDiagnostic] 流处理异常终止 after {} chunks, {}ms: {}",
                    chunkCount.get(), elapsed, e.getMessage(), e);
            // 向前端推送错误事件
            publisher.error(ctx, "模型响应流异常: " + e.getMessage());
        }

        // 钩子：流结束后的收尾
        afterStream(ctx);
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
        // 1. 从最后一条 ChatResponse 提取 token 统计，增量合并到 ctx.tokenStatists
        extractAndMergeTokenStats(ctx);

        // 2. 增量持久化到数据库
        persistTokenStats(ctx);

        // 3. 发射 metadata 事件给前端
        if (ctx.getTokenStatists() != null) {
            publisher.metadata(ctx, ctx.getTokenStatists());
        }
    }

    // ==================== 通用 token 提取与持久化（AOT 安全，零反射） ====================

    /**
     * 从 ChatResponse metadata 提取 token 统计，增量合并到 {@link AgentChatContext#getTokenStatists()}。
     * <p>
     * 使用 Spring AI 2.0.0 编译时类型安全 API：
     * <pre>
     *   ChatResponseMetadata → getUsage() → Usage
     * </pre>
     * 费用计算复用 {@link PriceCalculator#calculate(int, int, int, int, double, double, double, double)}，
     * 价格来源优先使用 {@link AgentChatContext#getUseModelInfo()} 中的定价字段。
     */
    private void extractAndMergeTokenStats(AgentChatContext ctx) {
        ChatResponse last = ctx.getLastResponse();
        if (last == null) return;

        ChatResponseMetadata responseMetadata = last.getMetadata();
        if (responseMetadata == null) return;

        Usage usage = responseMetadata.getUsage();
        if (usage == null) return;

        AgentTokenStatists stats = ctx.getTokenStatists();
        if (stats == null) {
            stats = new AgentTokenStatists();
            ctx.setTokenStatists(stats);
        }

        // 标准 token 字段（Usage.getXxx() 返回 int）
        stats.setPromptTokens(stats.getPromptTokens() != null ? stats.getPromptTokens() + usage.getPromptTokens() : usage.getPromptTokens());
        stats.setCompletionTokens(stats.getCompletionTokens() != null ? stats.getCompletionTokens() + usage.getCompletionTokens() : usage.getCompletionTokens());
        stats.setTotalTokens(stats.getTotalTokens() != null ? stats.getTotalTokens() + usage.getTotalTokens() : usage.getTotalTokens());

        // 缓存字段（Usage.getXxx() 返回 Long，可为 null）
        Long cacheRead = usage.getCacheReadInputTokens();
        Long cacheWrite = usage.getCacheWriteInputTokens();
        int cacheReadTokens = cacheRead != null ? cacheRead.intValue() : 0;
        int cacheWriteTokens = cacheWrite != null ? cacheWrite.intValue() : 0;
        stats.setCacheReadInputTokens(cacheReadTokens + (stats.getCacheReadInputTokens() != null ? stats.getCacheReadInputTokens() : 0));
        stats.setCacheWriteInputTokens(cacheWriteTokens + (stats.getCacheWriteInputTokens() != null ? stats.getCacheWriteInputTokens() : 0));

        // 预估费用：复用 PriceCalculator（与 chat 模块相同的计算逻辑）
        AgentModelInfo modelInfo = ctx.getUseModelInfo();
        if (modelInfo != null && modelInfo.getInputPrice() != null && modelInfo.getOutputPrice() != null) {
            double cost = PriceCalculator.calculate(
                    usage.getPromptTokens(), usage.getCompletionTokens(),
                    cacheReadTokens, cacheWriteTokens,
                    modelInfo.getInputPrice(), modelInfo.getOutputPrice(),
                    modelInfo.getCacheInputPrice() != null ? modelInfo.getCacheInputPrice() : modelInfo.getInputPrice(),
                    modelInfo.getCacheWriteInputPrice() != null ? modelInfo.getCacheWriteInputPrice() : modelInfo.getInputPrice());
            stats.setPriceEstimate((stats.getPriceEstimate() != null ? stats.getPriceEstimate() : 0.0) + cost);
            stats.setCurrency(modelInfo.getCurrency());
        }

        // 耗时（从请求开始时间到当前）
        long now = System.currentTimeMillis();
        int elapsedMs = (int) (now - ctx.getStartTime());
        stats.setDurationMs((stats.getDurationMs() != null ? stats.getDurationMs() : 0) + elapsedMs);
    }

    /**
     * 增量持久化 token 统计到数据库
     */
    private void persistTokenStats(AgentChatContext ctx) {
        AgentTokenStatists stats = ctx.getTokenStatists();
        if (stats == null) return;
        Long agentMsgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (agentMsgId == null) return;

        agentMsgDao.updateTokenIncremental(
                agentMsgId,
                stats.getPromptTokens() != null ? stats.getPromptTokens() : 0,
                stats.getCompletionTokens() != null ? stats.getCompletionTokens() : 0,
                stats.getTotalTokens() != null ? stats.getTotalTokens() : 0,
                stats.getCacheReadInputTokens() != null ? stats.getCacheReadInputTokens() : 0,
                stats.getCacheWriteInputTokens() != null ? stats.getCacheWriteInputTokens() : 0,
                stats.getCurrency() != null ? stats.getCurrency() : "",
                stats.getPriceEstimate() != null ? stats.getPriceEstimate() : 0.0,
                stats.getDurationMs() != null ? stats.getDurationMs() : 0);
    }
}
