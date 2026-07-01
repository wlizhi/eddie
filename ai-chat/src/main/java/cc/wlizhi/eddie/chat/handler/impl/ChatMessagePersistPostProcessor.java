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

    @Override
    public void process(ChatContext ctx) {
        Long placeholderMsgId = ctx.getPlaceholderMsgId();
        if (placeholderMsgId == null) {
            return;
        }

        String fullAnswer = ctx.getFullAnswer() != null ? ctx.getFullAnswer().toString() : "";
        String fullThinking = ctx.getFullThinking() != null ? ctx.getFullThinking().toString() : "";

        // 序列化工具调用记录
        String toolCallsJson = "[]";
        if (ctx.getToolCalls() != null && !ctx.getToolCalls().isEmpty()) {
            try {
                toolCallsJson = objectMapper.writeValueAsString(ctx.getToolCalls());
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        // 从上下文获取元数据
        MetadataInfo metadata = ctx.getMetadata();
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        int cacheReadInputTokens = 0;
        int cacheWriteInputTokens = 0;
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

        // 确定消息状态：中断 or 完成
        String msgStatus = ctx.isInterrupted() ? "INTERRUPTED" : "COMPLETED";

        // 更新占位消息为实际内容
        messageDao.updateAssistantMsg(
                placeholderMsgId, fullAnswer, fullThinking, toolCallsJson,
                promptTokens, completionTokens, totalTokens,
                cacheReadInputTokens, cacheWriteInputTokens,
                currency, priceEstimate, durationMs,
                msgStatus
        );

        // 更新会话的累计 token 数（message_count 已在初始插入时 +2，此处只更新 token）
        sessionDao.touchAndIncrementMessageCount(ctx.getSession().getId(), 0, totalTokens);
    }
}
