package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.dto.ModelParams;
import cc.wlizhi.eddieai.chat.service.ChatClientFactory;
import cc.wlizhi.eddieai.common.entity.AssistantEntity;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DeepseekChatClientFactory implements ChatClientFactory {

    private static final Logger log = LoggerFactory.getLogger(DeepseekChatClientFactory.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> supports = Set.of("deepseek", "dashscope");

    @Override
    public boolean support(String providerCode) {
        return supports.contains("deepseek");
    }

    @Override
    public ChatClient getChatClient(ChatContext ctx) {
        ModelProviderEntity provider = ctx.getProvider();
        AssistantEntity assistant = ctx.getAssistant();

        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(ctx.getOriginalRequest().getModelId());

        applyModelParams(optionsBuilder, assistant.getModelParams());

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .apiKey(provider.getApiKey())
                        .baseUrl(provider.getBaseUrl())
                        .build())
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    /**
     * 将 modelParams JSON 映射到 DeepSeekChatOptions.Builder，并提取 thinking 配置。
     */
    private void applyModelParams(DeepSeekChatOptions.Builder builder, String modelParamsJson) {
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
