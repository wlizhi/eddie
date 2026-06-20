/**
 * DefaultChatPreProcessor — 默认聊天请求预处理器
 * <p>
 * 职责：
 * 1. 校验并查询 ModelProvider 信息
 * 2. DTO 转换（ChatRequest → ChatClientGetDTO）
 * 3. 设置 SystemPrompt（TODO: 后续从 assistant 表获取）
 * 4. 填充基础上下文字段
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddieai.chat.mapper.ChatRequestMapper;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DefaultChatPreProcessor implements ChatPreProcessor {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ChatRequestMapper chatRequestMapper;

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

        // 2. DTO 转换
        ChatClientGetDTO dto = chatRequestMapper.toDto(request);
        ctx.setChatClientGetDTO(dto);

        // 3. System Prompt
        // TODO: 从 assistant 表获取 systemPrompt，当前硬编码占位
        ctx.setSystemPrompt("你是小麦，全名麦泱，一个年轻邻家女孩儿形象，说话甜甜的，让人觉得有点初恋感。你的责任是给用户提供准确的信息，帮助用户处理事务。回复内容要简洁明了、逻辑清晰，语气亲和友好。禁止长篇大论，禁止重复啰嗦、一本正经的胡说八道，禁止暴露你是AI的身份。");

        // 4. 基础字段
        ctx.setConversationId(request.getConversationId());
        ctx.setUserMessage(request.getMessage());
    }
}
