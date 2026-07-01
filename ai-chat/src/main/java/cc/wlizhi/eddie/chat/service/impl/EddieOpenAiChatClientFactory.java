/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Primary
@Service
public class EddieOpenAiChatClientFactory implements ChatClientFactory {

    private static final Logger log = LoggerFactory.getLogger(EddieOpenAiChatClientFactory.class);

    private static final Set<String> DEEPSEEK_CODES = Set.of("deepseek", "dashscope", "openai");

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean support(String providerCode) {
        return DEEPSEEK_CODES.contains(providerCode);
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
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getOriginalRequest().getModelId())
                .timeout(Duration.ofMinutes(10));

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
            case "low" -> applyReasoningEffort(builder, "low", providerCode);
            case "medium" -> applyReasoningEffort(builder, "medium", providerCode);
            case "high" -> applyReasoningEffort(builder, "high", providerCode);
            case "max" -> applyReasoningEffort(builder, "max", providerCode);
        }
    }

    /**
     * 设置 reasoningEffort，如果是 DeepSeek 协议还需同时传 thinking.type=enabled
     */
    private void applyReasoningEffort(EddieOpenAiChatOptions.Builder builder, String effort, String providerCode) {
        builder.reasoningEffort(effort);
        if (isDeepSeek(providerCode)) {
            builder.extraBody(Map.of("thinking", Map.of("type", "enabled")));
        }
    }

    private static boolean isDeepSeek(String providerCode) {
        return DEEPSEEK_CODES.contains(providerCode);
    }
}
