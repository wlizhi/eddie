/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiOptionsHelper;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Primary
@Slf4j
@Service
public class EddieOpenAiChatClientFactory implements ChatClientFactory {

    @Resource
    private EddieOpenAiOptionsHelper optionsHelper;

    @Override
    public boolean support(String providerCode) {
        return optionsHelper.supports(providerCode);
    }

    @Override
    public ChatClient getChatClient(ChatContext ctx) {
        ModelProviderEntity provider = ctx.getProvider();
        AssistantEntity assistant = ctx.getAssistant();

        // OkHttp 默认 read timeout 为 60 秒，长思考/长回答模型可能触发
        // AbstractOpenAiOptions.DEFAULT_TIMEOUT，导致 stream was reset: CANCEL。
        // 设 10 分钟确保完整输出。
        // TODO 这个超时事件后面改为可配置或用户页面设置，后端请求超时应当给出友好提示。
        EddieOpenAiChatOptions.Builder optionsBuilder = EddieOpenAiChatOptions.builder()
                .streamUsage(true)
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getOriginalRequest().getModelId())
                .timeout(Duration.ofMinutes(10));

        // 应用助手级 modelParams
        optionsHelper.applyModelParams(optionsBuilder, assistant.getModelParams(), ctx.getProviderCode());

        // 请求级 thinkingMode 覆盖助手配置
        String requestThinkingMode = ctx.getOriginalRequest().getThinkingMode();
        if (requestThinkingMode != null && !requestThinkingMode.isBlank()) {
            optionsHelper.applyThinkingMode(optionsBuilder, requestThinkingMode, ctx.getProviderCode());
        }

        EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }
}
