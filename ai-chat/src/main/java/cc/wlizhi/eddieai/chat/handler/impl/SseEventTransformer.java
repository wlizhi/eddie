/**
 * SseEventTransformer — SSE 事件转换器
 * <p>
 * 职责：将 ChatResponse 流转换为 SSE 事件流，包含三类事件：
 * <ul>
 *   <li>event: thinking — 模型思考内容，由 ThinkingHandler 按 providerCode 匹配提取</li>
 *   <li>event: answer — 模型回答内容，通用提取逻辑</li>
 *   <li>event: metadata — 流结束后构建的元数据（耗时、Token 用量等），由 MetadataHandler 处理</li>
 * </ul>
 */
package cc.wlizhi.eddieai.chat.handler.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.handler.MetadataHandler;
import cc.wlizhi.eddieai.chat.handler.ThinkingHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SseEventTransformer {

    @Resource
    private List<ThinkingHandler> thinkingHandlers;

    @Resource
    private List<MetadataHandler> metadataHandlers;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 ChatResponse 流转换为 SSE 事件流
     */
    public Flux<ServerSentEvent<String>> transform(Flux<ChatResponse> responseFlux, ChatContext ctx) {
        long startTime = ctx.getStartTime();

        return responseFlux
                .concatMap(response -> {
                    ServerSentEvent<String> thinkEvent = buildThinkEvent(response, ctx);
                    ServerSentEvent<String> answerEvent = buildAnswerEvent(response);
                    return Flux.fromIterable(
                            Stream.of(thinkEvent, answerEvent)
                                    .filter(Objects::nonNull)
                                    .toList()
                    );
                })
                .concatWith(
                        Mono.fromSupplier(() -> {
                            long endTime = System.currentTimeMillis();
                            return buildMetadataEvent(ctx, startTime, endTime);
                        })
                );
    }

    /**
     * 构建 thinking 事件 — 按 providerCode 匹配 ThinkingHandler
     * <p>
     * ThinkingHandler 同时负责提取和处理（因为提取逻辑是 provider-specific 的）。
     */
    private ServerSentEvent<String> buildThinkEvent(ChatResponse response, ChatContext ctx) {
        for (ThinkingHandler handler : thinkingHandlers) {
            if (handler.support(ctx.getProviderCode())) {
                String thinking = handler.extractThinking(response, ctx);
                if (ObjectUtils.isEmpty(thinking)) {
                    return null;
                }
                return ServerSentEvent.<String>builder()
                        .event("thinking")
                        .data(thinking)
                        .build();
            }
        }
        return null;
    }

    /**
     * 构建 answer 事件（通用逻辑）
     */
    private ServerSentEvent<String> buildAnswerEvent(ChatResponse response) {
        String content = response.getResults().stream()
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .filter(f -> !ObjectUtils.isEmpty(f))
                .collect(Collectors.joining());
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }
        return ServerSentEvent.<String>builder()
                .event("answer")
                .data(content)
                .build();
    }

    /**
     * 构建 metadata 事件
     */
    private ServerSentEvent<String> buildMetadataEvent(ChatContext ctx, long startTime, long endTime) {
        long durationMs = endTime - startTime;

        // 优先使用 MetadataHandler
        for (MetadataHandler handler : metadataHandlers) {
            if (handler.support(ctx.getProviderCode())) {
                Map<String, Object> data = handler.buildMetadata(ctx);
                data.putIfAbsent("durationMs", durationMs);
                data.putIfAbsent("timestamp", endTime);
                return ServerSentEvent.<String>builder()
                        .event("metadata")
                        .data(toJson(data))
                        .build();
            }
        }

        // fallback：默认元数据
        Map<String, Object> data = buildDefaultMetadata(ctx, durationMs, endTime);
        return ServerSentEvent.<String>builder()
                .event("metadata")
                .data(toJson(data))
                .build();
    }

    private Map<String, Object> buildDefaultMetadata(ChatContext ctx, long durationMs, long endTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("durationMs", durationMs);
        data.put("timestamp", endTime);

        ChatResponse lastResponse = ctx.getLastResponse();
        if (lastResponse != null) {
            ChatResponseMetadata metadata = lastResponse.getMetadata();
            Usage usage = metadata.getUsage();
            if (usage.getPromptTokens() != null) data.put("promptTokens", usage.getPromptTokens());
            if (usage.getCompletionTokens() != null) data.put("completionTokens", usage.getCompletionTokens());
            if (usage.getTotalTokens() != null) data.put("totalTokens", usage.getTotalTokens());
        }
        return data;
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
