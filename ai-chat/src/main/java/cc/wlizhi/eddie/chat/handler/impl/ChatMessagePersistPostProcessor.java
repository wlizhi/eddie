package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddie.common.dao.MessageDao;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.MessageEntity;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 聊天消息持久化后置处理器：异步将 user/assistant 消息写入 DB，更新会话活跃时间
 */
@Component
public class ChatMessagePersistPostProcessor implements ChatPostProcessor {

    @Resource
    private SessionDao sessionDao;

    @Resource
    private MessageDao messageDao;

    @Override
    public void process(ChatContext ctx) {
        CompletableFuture.runAsync(() -> persist(ctx));
    }

    private void persist(ChatContext ctx) {
        SessionEntity session = ctx.getSession();
        if (session == null) {
            return;
        }
        Long sessionId = session.getId();
        Long assistantId = session.getAssistantId();
        ChatRequest request = ctx.getOriginalRequest();
        String toolCallsJson = "[]";

        // 持久化 user 消息
        MessageEntity userMsg = new MessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setAssistantId(assistantId);
        userMsg.setRole("user");
        userMsg.setContent(ctx.getUserMessage());
        userMsg.setProviderId(request.getProviderId());
        userMsg.setModelCode(request.getModelId());
        userMsg.setModelName(request.getModelId());
        userMsg.setThinking("");
        userMsg.setPromptTokens(0);
        userMsg.setCompletionTokens(0);
        userMsg.setTotalTokens(0);
        userMsg.setPriceEstimate(0.0);
        // 货币符号
        ModelPricing pricing = ctx.getPricing();
        String currency = pricing != null && pricing.getCurrency() != null ? pricing.getCurrency() : "";
        userMsg.setCurrency(currency);
        userMsg.setToolCalls(toolCallsJson);
        messageDao.insert(userMsg);

        // 持久化 assistant 消息
        MessageEntity assistantMsg = new MessageEntity();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setAssistantId(assistantId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(ctx.getFullAnswer() != null ? ctx.getFullAnswer().toString() : "");
        assistantMsg.setProviderId(request.getProviderId());
        assistantMsg.setModelCode(request.getModelId());
        assistantMsg.setModelName(request.getModelId());
        assistantMsg.setThinking(ctx.getFullThinking() != null ? ctx.getFullThinking().toString() : "");
        // 持久化工具调用记录
        if (ctx.getToolCalls() != null && !ctx.getToolCalls().isEmpty()) {
            try {
                toolCallsJson = new ObjectMapper().writeValueAsString(ctx.getToolCalls());
            } catch (JsonProcessingException e) {
                // ignore
            }
        }
        assistantMsg.setToolCalls(toolCallsJson);

        // 从上下文获取经过 handler 处理的元数据（单一数据源）
        MetadataInfo metadata = ctx.getMetadata();
        int totalTokens = 0;
        if (metadata != null) {
            assistantMsg.setPromptTokens(metadata.getPromptTokens());
            assistantMsg.setCompletionTokens(metadata.getCompletionTokens());
            assistantMsg.setTotalTokens(metadata.getTotalTokens());
            assistantMsg.setCacheReadInputTokens(metadata.getCacheReadInputTokens());
            assistantMsg.setCacheWriteInputTokens(metadata.getCacheWriteInputTokens());
            assistantMsg.setCurrency(metadata.getCurrency() != null ? metadata.getCurrency() : currency);
            assistantMsg.setPriceEstimate(metadata.getCostEstimate());
            assistantMsg.setDurationMs((int) metadata.getDurationMs());
            totalTokens = metadata.getTotalTokens();
        } else {
            assistantMsg.setPromptTokens(0);
            assistantMsg.setCompletionTokens(0);
            assistantMsg.setTotalTokens(0);
            assistantMsg.setCacheReadInputTokens(0);
            assistantMsg.setCacheWriteInputTokens(0);
            assistantMsg.setCurrency(currency);
            assistantMsg.setPriceEstimate(0.0);
        }
        messageDao.insert(assistantMsg);

        // 更新会话活跃时间并同步消息数量、累计 token 数（合并为一条 SQL）
        sessionDao.touchAndIncrementMessageCount(sessionId, 2, totalTokens);
    }
}
