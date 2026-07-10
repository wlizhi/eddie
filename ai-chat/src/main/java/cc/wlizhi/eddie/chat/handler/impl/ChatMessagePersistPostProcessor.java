/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddie.common.dao.MessageDao;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 聊天消息后置处理器：流结束后更新占位 assistant 消息为实际内容
 * <p>
 * user 消息和占位 assistant 消息已在流开始前由 {@link ChatServiceImpl#persistInitialMessages}
 * 事务性写入 DB。本处理器仅负责 UPDATE。
 */
@Component
public class ChatMessagePersistPostProcessor implements ChatPostProcessor {

    @Resource
    private SessionDao sessionDao;

    @Resource
    private MessageDao messageDao;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void process(ChatContext ctx) {
        Long placeholderMsgId = ctx.getPlaceholderMsgId();
        if (placeholderMsgId == null) {
            return;
        }

        String fullAnswer = ctx.getFullAnswer() != null ? ctx.getFullAnswer().toString() : "";
        String fullThinking = ctx.getFullThinking() != null ? ctx.getFullThinking().toString() : "";
        String toolCallsJson = serializeToolCalls(ctx.getToolCalls());
        Long userMsgId = ctx.getUserMessageId();
        String msgStatus = ctx.isInterrupted() ? "INTERRUPTED" : "COMPLETED";

        // 回填 round_seq = userMsgId（user 和 assistant 共用同一值），事务保证原子性
        MetadataInfo metadata = ctx.getMetadata();
        transactionTemplate.executeWithoutResult(status -> {
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;
            int cacheReadInputTokens = 0, cacheWriteInputTokens = 0;
            String currency = "";
            double priceEstimate = 0.0;
            int durationMs = 0;

            if (metadata != null) {
                promptTokens = metadata.getPromptTokens();
                completionTokens = metadata.getCompletionTokens();
                totalTokens = metadata.getTotalTokens();
                cacheReadInputTokens = metadata.getCacheReadInputTokens();
                cacheWriteInputTokens = metadata.getCacheWriteInputTokens();
                currency = metadata.getCurrency() != null ? metadata.getCurrency() : "";
                priceEstimate = metadata.getCostEstimate();
                durationMs = (int) metadata.getDurationMs();
            } else {
                ModelPricing pricing = ctx.getPricing();
                currency = pricing != null && pricing.getCurrency() != null ? pricing.getCurrency() : "";
            }

            messageDao.updateAssistantMsg(
                    placeholderMsgId, fullAnswer, fullThinking, toolCallsJson,
                    promptTokens, completionTokens, totalTokens,
                    cacheReadInputTokens, cacheWriteInputTokens,
                    currency, priceEstimate, durationMs,
                    msgStatus, userMsgId
            );
            // 同时更新 user 消息的 round_seq
            messageDao.updateRoundSeq(userMsgId, userMsgId);

            // 更新会话的累计 token 数（message_count 已在初始插入时 +2，此处只更新 token）
            sessionDao.touchAndIncrementMessageCount(ctx.getSession().getId(), 0, totalTokens);
        });
    }

    /**
     * 序列化工具调用记录为 JSON 字符串
     */
    private String serializeToolCalls(List<ToolExecutionEvent> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(toolCalls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
