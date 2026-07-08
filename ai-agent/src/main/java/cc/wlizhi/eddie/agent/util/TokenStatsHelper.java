/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.util;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import cc.wlizhi.eddie.common.util.PriceCalculator;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Token 统计工具类 — 统一提取、合并与持久化逻辑。
 * <p>
 * 将 {@link cc.wlizhi.eddie.agent.handler.processor.AbstractStreamProcessor} 和
 * {@link cc.wlizhi.eddie.agent.handler.processor.AbstractBlockingProcessor}
 * 中重复的 token 提取与持久化逻辑收拢至此，避免代码复制。
 * <p>
 * 本类为纯静态工具方法，无状态、零反射，AOT 安全。
 */
public final class TokenStatsHelper {

    private TokenStatsHelper() {
    }

    /**
     * 从 {@link ChatResponse} metadata 提取 token 统计，增量合并到
     * {@link AgentChatContext#getTokenStatists()}。
     * <p>
     * 使用 Spring AI 2.0.0 编译时类型安全 API：
     * {@code ChatResponseMetadata → getUsage() → Usage}
     * <p>
     * 费用计算复用 {@link PriceCalculator#calculate(int, int, int, int, double, double, double, double)}，
     * 价格来源优先使用 {@link AgentChatContext#getUseModelInfo()} 中的定价字段。
     *
     * @param ctx 当前请求上下文（其 lastResponse 必须有值）
     */
    public static void extractAndMergeTokenStats(AgentChatContext ctx) {
        ChatResponse last = ctx.getLastResponse();
        if (last == null) return;

        ChatResponseMetadata responseMetadata = last.getMetadata();

        Usage usage = responseMetadata.getUsage();

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
        stats.setDurationMs(elapsedMs);
    }

    /**
     * 增量持久化 token 统计到数据库。
     *
     * @param ctx        当前请求上下文
     * @param agentMsgDao AgentMsgDao 实例
     */
    public static void persistTokenStats(AgentChatContext ctx, AgentMsgDao agentMsgDao) {
        AgentTokenStatists stats = ctx.getTokenStatists();
        if (stats == null) return;
        Long agentMsgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (agentMsgId == null) return;

        agentMsgDao.updateTokenAbsolute(
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
