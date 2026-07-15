/**
 * @author Eddie
 * {@code @date} 2026-07-14
 */

package cc.wlizhi.eddie.tools.service;

import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiOptionsHelper;
import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.dto.ModelSelection;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.memory.context.BuiltInPromptsContext;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页抓取内容摘要服务。
 * <p>
 * 当网页内容超过 {@code maxChars} 时，按降级链调用 LLM 生成摘要替代硬截断。
 * 降级链：{@link GlobalConfigKey#FAST_MODEL FAST_MODEL} → {@link GlobalConfigKey#DEFAULT_MODEL DEFAULT_MODEL} → 返回 null（调用方自行截断）。
 */
@Slf4j
@Component
public class WebFetchSummarizer {

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private BuiltInPromptsContext builtInPromptsContext;

    @Resource
    private EddieOpenAiOptionsHelper optionsHelper;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 对超限内容生成摘要。
     *
     * @param content  原始网页 Markdown（已超过 maxChars）
     * @param purpose  关注方向，由调用方传入（可为 null），如 "技术细节、价格信息"
     * @param maxChars 摘要目标长度
     * @return 摘要文本；如果未配置任何模型则返回 null（由调用方决定截断）
     */
    @Nullable
    public String summarize(String content, @Nullable String purpose, int maxChars) {
        // 降级链 1: FAST_MODEL（轻量快速模型，如 gpt-4o-mini）
        String summary = tryModel(GlobalConfigKey.FAST_MODEL, content, purpose, maxChars);
        if (summary != null) return summary;

        // 降级链 2: DEFAULT_MODEL（默认对话模型）
        summary = tryModel(GlobalConfigKey.DEFAULT_MODEL, content, purpose, maxChars);
        if (summary != null) return summary;

        // 都未配置 → 返回 null，调用方走截断
        return null;
    }

    @Nullable
    private String tryModel(GlobalConfigKey configKey, String content,
                            @Nullable String purpose, int maxChars) {
        ModelSelection config = globalConfigContext.getConfig(configKey, new TypeReference<ModelSelection>() {
        });
        if (config == null || config.getProviderId() == null || config.getModelId() == null) {
            return null;
        }

        ModelProviderEntity provider = modelProviderContext.getModelProviderById(config.getProviderId());
        if (provider == null) {
            log.warn("[WebFetchSummarizer] {} provider 不存在: {}", configKey.name(), config.getProviderId());
            return null;
        }

        try {
            EddieOpenAiChatOptions.Builder optionsBuilder = EddieOpenAiChatOptions.builder()
                    .baseUrl(provider.getBaseUrl())
                    .apiKey(provider.getApiKey())
                    .model(config.getModelId())
                    .timeout(Duration.ofSeconds(30));

            // 应用全局配置中的模型参数（temperature、topP、maxTokens 等）
            ModelParams modelParams = config.getModelParams();
            if (modelParams != null) {
                String modelParamsJson = objectMapper.writeValueAsString(modelParams);
                optionsHelper.applyModelParams(optionsBuilder, modelParamsJson, provider.getCode());
            }

            EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                    .options(optionsBuilder.build())
                    .build();

            // 从外置 prompt 模板构建系统提示词
            String sysMsgTemplate = builtInPromptsContext.getWebFetchSummaryPrompts();
            if (sysMsgTemplate == null) {
                log.warn("[WebFetchSummarizer] webFetchSummaryPrompts 未加载，使用降级");
                sysMsgTemplate = "你是一个专业的网页内容摘要助手。";
            }
            Map<String, String> variables = new HashMap<>();
            variables.put("purpose", purpose != null && !purpose.isBlank() ? purpose : "");
            variables.put("maxChars", String.valueOf(maxChars));
            String sysMsg = builtInPromptsContext.resolvePrompt(sysMsgTemplate, variables);

            // 构建用户提示词 — 意图导向的摘要策略
            String userMsg;
            if (purpose != null && !purpose.isBlank()) {
                userMsg = "请总结以下网页内容。重点关注与「" + purpose + "」相关的信息，"
                        + "保留所有关键细节和数据；与关注方向无关的内容可简要概括或省略。"
                        + "\n\n" + content;
            } else {
                userMsg = "请总结以下网页内容，提炼关键事实、数据和结论。"
                        + "\n\n" + content;
            }

            Generation generation = chatModel.call(
                    new Prompt(List.of(
                            new SystemMessage(sysMsg),
                            new UserMessage(userMsg)
                    ))
            ).getResult();
            if (generation == null) {
                log.warn("[WebFetchSummarizer] {} 摘要失败: generation is null", configKey.name());
                return content;
            }
            String result = generation.getOutput().getText();

            log.info("[WebFetchSummarizer] {} 摘要完成: {} chars → {} chars",
                    configKey.name(), content.length(), result == null ? null : result.length());
            return result;

        } catch (Exception e) {
            log.warn("[WebFetchSummarizer] {} 摘要失败: {}", configKey.name(), e.getMessage());
            return null; // 降级到下一级
        }
    }
}
