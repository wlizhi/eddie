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
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Set;

@Primary
@Service
public class EddieOpenAiChatClientFactory implements ChatClientFactory {

    private static final Logger log = LoggerFactory.getLogger(EddieOpenAiChatClientFactory.class);

    private static final Set<String> DEEPSEEK_CODES = Set.of("deepseek", "dashscope", "openai");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean support(String providerCode) {
        return DEEPSEEK_CODES.contains(providerCode);
    }

    @Override
    public ChatClient getChatClient(ChatContext ctx) {
        ModelProviderEntity provider = ctx.getProvider();
        AssistantEntity assistant = ctx.getAssistant();

        EddieOpenAiChatOptions.Builder optionsBuilder = EddieOpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getOriginalRequest().getModelId());

        // 应用助手级 modelParams
        applyModelParams(optionsBuilder, assistant.getModelParams(), ctx.getProviderCode());

        // 请求级 thinkingMode 覆盖助手配置
        String requestThinkingMode = ctx.getOriginalRequest().getThinkingMode();
        if (requestThinkingMode != null && !requestThinkingMode.isBlank()) {
            applyThinkingMode(optionsBuilder, requestThinkingMode, ctx.getProviderCode());
        }

        EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private void applyModelParams(EddieOpenAiChatOptions.Builder builder, String modelParamsJson, String providerCode) {
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

            // 助手级思考参数
            String thinkingMode = params.getThinkingMode();
            if (!ObjectUtils.isEmpty(thinkingMode)) {
                applyThinkingMode(builder, thinkingMode, providerCode);
            }
        } catch (Exception e) {
            log.warn("解析助手 modelParams JSON 失败，将使用默认参数。json={}", modelParamsJson, e);
        }
    }

    /**
     * 将思考模式值映射到实际的 API 参数
     *
     * @param mode auto / disabled / low / medium / high / max
     */
    private void applyThinkingMode(EddieOpenAiChatOptions.Builder builder, String mode, String providerCode) {
        if (ObjectUtils.isEmpty(mode)) {
            return;
        }
        switch (mode) {
            case "auto" -> {
                // 不传任何参数，模型决定
            }
            case "disabled" -> {
                // 不设 reasoningEffort
                if (isDeepSeek(providerCode)) {
                    builder.extraBody(Map.of("thinking", Map.of("type", "disabled")));
                }
            }
            case "low" -> builder.reasoningEffort("low");
            case "medium" -> builder.reasoningEffort("medium");
            case "high" -> builder.reasoningEffort("high");
            case "max" -> {
                if (isDeepSeek(providerCode)) {
                    builder.reasoningEffort("max");
                    if (isDeepSeek(providerCode)) {
                        builder.extraBody(Map.of("thinking", Map.of("type", "enabled")));
                    }
                } else {
                    builder.reasoningEffort("high");
                }
            }
        }
    }

    private static boolean isDeepSeek(String providerCode) {
        return DEEPSEEK_CODES.contains(providerCode);
    }
}
