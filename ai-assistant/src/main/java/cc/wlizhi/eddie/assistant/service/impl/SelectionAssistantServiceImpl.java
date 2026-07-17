/**
 * @author Eddie
 * {@code @date} 2026-07-15
 */

package cc.wlizhi.eddie.assistant.service.impl;

import cc.wlizhi.eddie.assistant.entity.request.SelectionAssistantRequest;
import cc.wlizhi.eddie.assistant.service.SelectionAssistantService;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiOptionsHelper;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.dto.ModelSelection;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.exception.AppException;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.concurrent.CancellationException;

/**
 * 划词助手业务实现
 * <p>
 * 根据 action 自动解析模型配置，以 SSE 流式返回 AI 处理结果。
 * 模型选择逻辑：
 * <ul>
 *   <li>translate → TRANSLATE_MODEL</li>
 *   <li>summarize / explain / beautify → FAST_MODEL → DEFAULT_MODEL（降级）</li>
 * </ul>
 */
@Service
public class SelectionAssistantServiceImpl implements SelectionAssistantService {

    private static final Logger log = LoggerFactory.getLogger(SelectionAssistantServiceImpl.class);

    private static final TypeReference<ModelSelection> MODEL_SEL_REF = new TypeReference<>() {
    };

    @Resource
    private GlobalConfigContext globalConfig;

    @Resource
    private ModelProviderContext providerContext;

    @Resource
    private EddieOpenAiOptionsHelper optionsHelper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Flux<ServerSentEvent<String>> stream(SelectionAssistantRequest request) {
        return Mono.fromCallable(() -> {
                    // ===== 1. 解析模型选择 & 供应商配置（同步，纯内存操作） =====
                    ModelSelection modelSel = resolveModelSelection(request.getAction());
                    if (modelSel == null) {
                        throw new AppException(getModelNotConfiguredHint(request.getAction()));
                    }
                    ModelProviderEntity provider = providerContext.getModelProviderById(modelSel.getProviderId());
                    if (provider == null) {
                        throw new AppException("模型供应商未找到，请检查配置");
                    }
                    return buildChatClient(modelSel, provider);
                })
                .flux()
                .flatMap(client -> {
                    // ===== 2. 流式调用 → 映射为 SSE 事件（纯响应式管道） =====
                    long startMs = System.currentTimeMillis();
                    String systemPrompt = resolvePrompt(request);

                    Flux<ServerSentEvent<String>> deltaStream = client.prompt()
                            .system(systemPrompt)
                            .user(request.getText())
                            .stream()
                            .content()
                            .map(SelectionAssistantServiceImpl::buildDeltaEvent);

                    Flux<ServerSentEvent<String>> metadataEvent = Flux.just(
                            buildMetadataEvent(System.currentTimeMillis() - startMs));

                    return deltaStream.concatWith(metadataEvent);
                })
                .doFinally(signalType -> {
                    if (signalType == SignalType.CANCEL) {
                        log.info("客户端断开SSE连接: action={}", request.getAction());
                    }
                })
                .onErrorResume(CancellationException.class, e -> {
                    log.debug("流式处理被客户端取消: action={}, msg={}", request.getAction(), e.getMessage());
                    return Flux.empty();
                })
                .onErrorResume(e -> {
                    log.warn("划词助手处理异常: action={}, msg={}", request.getAction(), e.getMessage());
                    return Flux.just(buildErrorEvent(e.getMessage()));
                });
    }

    /**
     * 构建 ChatClient（纯内存操作）
     */
    private ChatClient buildChatClient(ModelSelection modelSel, ModelProviderEntity provider) throws Exception {
        EddieOpenAiChatOptions.Builder optBuilder = EddieOpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(modelSel.getModelId())
                .timeout(Duration.ofSeconds(60));

        if (modelSel.getModelParams() != null) {
            String modelParamsJson = objectMapper.writeValueAsString(modelSel.getModelParams());
            optionsHelper.applyModelParams(optBuilder, modelParamsJson, provider.getCode());
        }

        EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                .options(optBuilder.build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 根据 action 解析模型选择
     */
    private ModelSelection resolveModelSelection(String action) {
        if ("translate".equals(action)) {
            return globalConfig.getConfig(GlobalConfigKey.TRANSLATE_MODEL, MODEL_SEL_REF);
        }
        ModelSelection ms = globalConfig.getConfig(GlobalConfigKey.FAST_MODEL, MODEL_SEL_REF);
        if (ms == null) {
            ms = globalConfig.getConfig(GlobalConfigKey.DEFAULT_MODEL, MODEL_SEL_REF);
        }
        return ms;
    }

    private String getModelNotConfiguredHint(String action) {
        if ("translate".equals(action)) {
            return "请先在设置中配置翻译模型";
        }
        return "请先在设置中配置快速模型或默认模型";
    }

    /**
     * 根据 action 解析系统提示词
     */
    private String resolvePrompt(SelectionAssistantRequest request) {
        return switch (request.getAction()) {
            case "translate" -> String.format("""
                    你是一个专业翻译助手。必须将以下文本翻译成目标语言【%s】。
                    要求：
                    - 无论原文是什么语言，必须输出【%s】的翻译结果，严禁输出其他语言
                    - 保持原文的语气和风格
                    - 专业术语准确
                    - 只返回翻译结果，不要额外解释或说明
                    """,
                    request.getTargetLang() != null ? request.getTargetLang() : "中文",
                    request.getTargetLang() != null ? request.getTargetLang() : "中文");
            case "summarize" -> """
                    请对以下文本进行简洁的总结：
                    - 提取核心要点
                    - 使用简洁的语言
                    - 按要点分条列出（使用 Markdown 格式）
                    """;
            case "explain" -> """
                    请对以下文本进行详细解释：
                    - 解释关键概念和术语
                    - 提供相关背景知识
                    - 使用通俗易懂的语言
                    """;
            case "beautify" -> """
                    请美化以下文本：
                    - 修正语法和拼写错误
                    - 优化表达方式，使其更流畅自然
                    - 保持原意不变
                    - 使用更专业、优雅的措辞
                    """;
            default -> "请根据以下文本回答问题。";
        };
    }

    // ========== SSE 事件构建 ==========

    private static ServerSentEvent<String> buildDeltaEvent(String content) {
        return ServerSentEvent.<String>builder()
                .event("delta")
                .data("{\"content\":" + escapeJson(content) + "}")
                .build();
    }

    private static ServerSentEvent<String> buildMetadataEvent(long durationMs) {
        return ServerSentEvent.<String>builder()
                .event("metadata")
                .data("{\"durationMs\":" + durationMs + "}")
                .build();
    }

    private static ServerSentEvent<String> buildErrorEvent(String message) {
        return ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"message\":" + escapeJson(message) + "}")
                .build();
    }

    private static String escapeJson(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x10) {
                        sb.append("\\u00").append(Character.forDigit((c >> 4) & 0xF, 16))
                                .append(Character.forDigit(c & 0xF, 16));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
