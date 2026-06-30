/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.annotation.Order;

import java.util.Objects;

@Slf4j
@Order
//@Service
public class OpenAiChatClientFactory implements ChatClientFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "openai");
    }

    @Override
    public ChatClient getChatClient(ChatContext ctx) {
        ModelProviderEntity provider = ctx.getProvider();
        AssistantEntity assistant = ctx.getAssistant();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getOriginalRequest().getModelId());

        applyModelParams(optionsBuilder, assistant.getModelParams());

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private void applyModelParams(OpenAiChatOptions.Builder builder, String modelParamsJson) {
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
