/**
 * DefaultChatPreProcessor — 默认聊天请求预处理器
 * <p>
 * 职责：
 * 1. 校验并查询 ModelProvider 信息
 * 2. 根据 conversationId 查询会话 → 获取助手信息
 * 3. 填充基础上下文字段
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DefaultChatPreProcessor implements ChatPreProcessor {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private AssistantContext assistantContext;

    @Resource
    private SessionDao sessionDao;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void process(ChatContext ctx) {
        ChatRequest request = ctx.getOriginalRequest();

        // 1. 查询并校验 Provider
        ModelProviderEntity provider = modelProviderContext.getModelProviderById(request.getProviderId());
        if (provider == null) {
            throw new BadRequestException("providerId=" + request.getProviderId() + " 不存在的模型服务商");
        }
        if (Objects.equals(provider.getEnabled(), 0)) {
            throw new BadRequestException(provider.getName() + " 模型服务已禁用");
        }
        // 1.1 校验 API Key 是否已配置
        if (ObjectUtils.isEmpty(provider.getApiKey()) && Objects.equals(provider.getBuiltIn(), 1)) {
            throw new BadRequestException("请先为 [" + provider.getName() + "] 配置 API Key");
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
        if (Objects.equals(assistant.getEnabled(), 0)) {
            throw new BadRequestException(assistant.getName() + " 已禁用，请启用后重试");
        }
        ctx.setAssistant(assistant);
        ctx.setSession(session);

        // 3. 基础字段
        ctx.setUserMessage(request.getMessage());

        // 4. 解析模型价格（每百万 token 单价）：从 provider.models JSON 中匹配当前 modelId，提取 input_price / output_price / currency
        String modelsJson = provider.getModels();
        if (!ObjectUtils.isEmpty(modelsJson) && !"[]".equals(modelsJson)) {
            try {
                List<Map<String, Object>> modelList = objectMapper.readValue(
                        modelsJson, new TypeReference<List<Map<String, Object>>>() {
                        });
                modelList.stream()
                        .filter(m -> request.getModelId().equals(m.get("id")))
                        .findFirst()
                        .ifPresent(m -> {
                            ModelPricing pricing = new ModelPricing();
                            Object inputPrice = m.get("input_price");
                            Object outputPrice = m.get("output_price");
                            if (inputPrice instanceof Number)
                                pricing.setInputPrice(((Number) inputPrice).doubleValue());
                            if (outputPrice instanceof Number)
                                pricing.setOutputPrice(((Number) outputPrice).doubleValue());
                            // cache_input_price 为可选字段，后续模型设置中可配置
                            Object cacheInputPrice = m.get("cache_input_price");
                            if (cacheInputPrice instanceof Number)
                                pricing.setCacheInputPrice(((Number) cacheInputPrice).doubleValue());
                            // cache_write_input_price 为可选字段
                            Object cacheWriteInputPrice = m.get("cache_write_input_price");
                            if (cacheWriteInputPrice instanceof Number)
                                pricing.setCacheWriteInputPrice(((Number) cacheWriteInputPrice).doubleValue());
                            Object currency = m.get("currency");
                            if (currency instanceof String) pricing.setCurrency((String) currency);
                            ctx.setPricing(pricing);
                        });
            } catch (Exception e) {
                // 价格解析失败不影响主流程，仅跳过费用计算
                // log 由调用方统一处理
            }
        }
    }
}
