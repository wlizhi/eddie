package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Primary
@Service
public class EddieOpenAiChatClientFactory implements ChatClientFactory {

    private static final Logger log = LoggerFactory.getLogger(EddieOpenAiChatClientFactory.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean support(String providerCode) {
        // 走默认逻辑
        return Objects.equals(providerCode, "openai");
    }

    @Override
    public ChatClient getChatClient(ChatContext ctx) {
        ModelProviderEntity provider = ctx.getProvider();
        AssistantEntity assistant = ctx.getAssistant();

        EddieOpenAiChatOptions.Builder optionsBuilder = EddieOpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getOriginalRequest().getModelId());

        applyModelParams(optionsBuilder, assistant.getModelParams());

        EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private void applyModelParams(EddieOpenAiChatOptions.Builder builder, String modelParamsJson) {
        if (modelParamsJson == null || modelParamsJson.isBlank()) {
            return;
        }
        try {
            ModelParams params = objectMapper.readValue(modelParamsJson, ModelParams.class);
            if (params.getTemperature() != null) {
                builder.temperature(params.getTemperature());
            }
            if (params.getMaxTokens() != null) {
                builder.maxTokens(params.getMaxTokens());
            }
            if (params.getTopP() != null) {
                builder.topP(params.getTopP());
            }
            if (params.getFrequencyPenalty() != null) {
                builder.frequencyPenalty(params.getFrequencyPenalty());
            }
            if (params.getPresencePenalty() != null) {
                builder.presencePenalty(params.getPresencePenalty());
            }
            if (params.getStop() != null && !params.getStop().isEmpty()) {
                builder.stop(params.getStop());
            }
        } catch (Exception e) {
            log.warn("解析助手 modelParams JSON 失败，将使用默认参数。json={}", modelParamsJson, e);
        }
    }
}
