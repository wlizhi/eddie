/**
 * DefaultChatPreProcessor — 默认聊天请求预处理器
 * <p>
 * 职责：
 * 1. 校验并查询 ModelProvider 信息
 * 2. 根据 conversationId 查询会话 → 获取助手信息
 * 3. 填充基础上下文字段
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.context.AssistantContext;
import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddieai.common.dao.SessionDao;
import cc.wlizhi.eddieai.common.entity.AssistantEntity;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.entity.SessionEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DefaultChatPreProcessor implements ChatPreProcessor {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private AssistantContext assistantContext;

    @Resource
    private SessionDao sessionDao;

    @Override
    public void process(ChatContext ctx) {
        ChatRequest request = ctx.getOriginalRequest();

        // 1. 查询并校验 Provider
        ModelProviderEntity provider = modelProviderContext.getModelProviderById(request.getProviderId());
        if (provider == null) {
            throw new BadRequestException("providerId=" + request.getProviderId() + " 不存在的模型服务商");
        }
        ctx.setProvider(provider);
        ctx.setProviderCode(provider.getCode());

        // 2. 根据 conversationId 查询会话 → 获取助手 → 填充完整实体
        SessionEntity session = sessionDao.findById(request.getConversationId());
        if (session == null) {
            throw new BadRequestException("conversationId=" + request.getConversationId() + " 不存在的会话");
        }
        Long assistantId = session.getAssistantId();
        AssistantEntity assistant = assistantContext.getAssistantById(assistantId);
        if (assistant == null) {
            throw new BadRequestException("assistantId=" + assistantId + " 不存在的助手");
        }
        ctx.setAssistant(assistant);
        ctx.setSession(session);

        // 3. 基础字段
        ctx.setUserMessage(request.getMessage());
    }
}
