package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddieai.common.dao.MessageDao;
import cc.wlizhi.eddieai.common.dao.SessionDao;
import cc.wlizhi.eddieai.common.entity.MessageEntity;
import cc.wlizhi.eddieai.common.entity.SessionEntity;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
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

        ChatResponse lastResponse = ctx.getLastResponse();
        if (lastResponse != null) {
            ChatResponseMetadata metadata = lastResponse.getMetadata();
            Usage usage = metadata.getUsage();
            usage.getPromptTokens();
            assistantMsg.setPromptTokens(usage.getPromptTokens());
            usage.getCompletionTokens();
            assistantMsg.setCompletionTokens(usage.getCompletionTokens());
            usage.getTotalTokens();
            assistantMsg.setTotalTokens(usage.getTotalTokens());
        } else {
            assistantMsg.setPromptTokens(0);
            assistantMsg.setCompletionTokens(0);
            assistantMsg.setTotalTokens(0);
        }
        assistantMsg.setPriceEstimate(0.0);
        messageDao.insert(assistantMsg);

        // 更新会话活跃时间并同步消息数量（合并为一条 SQL）
        sessionDao.touchAndIncrementMessageCount(sessionId, 2);
    }
}
