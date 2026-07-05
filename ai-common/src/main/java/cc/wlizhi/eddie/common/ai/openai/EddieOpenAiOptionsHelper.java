package cc.wlizhi.eddie.common.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class EddieOpenAiOptionsHelper {
    private static final Set<String> SUPPORT_CODES = Set.of("deepseek", "dashscope", "openai");
    @Resource
    private ObjectMapper objectMapper;

    public void applyModelParams(EddieOpenAiChatOptions.Builder builder, String modelParamsJson, String providerCode) {
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
    public void applyThinkingMode(EddieOpenAiChatOptions.Builder builder, String mode, String providerCode) {
        if (ObjectUtils.isEmpty(mode)) {
            return;
        }
        switch (mode) {
            case "auto" -> {
                // 不传任何参数，模型决定
            }
            case "disabled" -> {
                // 不设 reasoningEffort
                if (supports(providerCode)) {
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
    public void applyReasoningEffort(EddieOpenAiChatOptions.Builder builder, String effort, String providerCode) {
        builder.reasoningEffort(effort);
        if (supports(providerCode)) {
            builder.extraBody(Map.of("thinking", Map.of("type", "enabled")));
        }
    }

    public boolean supports(String providerCode) {
        return SUPPORT_CODES.contains(providerCode);
    }
}
