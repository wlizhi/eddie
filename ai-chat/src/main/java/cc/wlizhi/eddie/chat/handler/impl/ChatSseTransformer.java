/**
 * ChatSseTransformer — SSE 事件转换器
 * <p>
 * 职责：将 ChatResponse 流转换为 SSE 事件流，包含四类事件：
 * <ul>
 *   <li>event: thinking — 模型思考内容，由 ChatThinkingHandler 按 providerCode 匹配提取</li>
 *   <li>event: answer — 模型回答内容，通用提取逻辑</li>
 *   <li>event: tool_execution — 工具执行状态（开始/完成），直接序列化 ToolExecutionEvent 实体</li>
 *   <li>event: metadata — 流结束后构建的元数据，直接序列化 MetadataInfo 实体</li>
 * </ul>
 * <p>
 * 所有结构化数据均通过实体类序列化，统一数据源，避免 Map 导致的数据分裂。
 */
package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.handler.ChatMetadataHandler;
import cc.wlizhi.eddie.chat.handler.ChatThinkingHandler;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class ChatSseTransformer {

    @Resource
    private List<ChatThinkingHandler> thinkingHandlers;

    @Resource
    private List<ChatMetadataHandler> metadataHandlers;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private GlobalConfigContext globalConfigContext;

    // 绝对上限（硬编码，不可超过）
    private static final int MAX_TOOL_CALL_RES_LENGTH = 8000;


    /**
     * 将 ChatResponse 流转换为 SSE 事件流，并合并工具执行事件
     *
     * @param responseFlux  ChatResponse 流
     * @param ctx           聊天上下文
     * @param toolEventFlux 工具执行事件流（可选，来自 ToolCallbackWrapper 的 Sinks 旁路）
     * @param toolEventSink 工具执行事件 Sink，在 mainSse 完成时自动关闭以释放合并流
     */
    public Flux<ServerSentEvent<String>> transform(Flux<ChatResponse> responseFlux, ChatContext ctx,
                                                   Flux<ToolExecutionEvent> toolEventFlux,
                                                   Sinks.Many<ToolExecutionEvent> toolEventSink) {
        long startTime = ctx.getStartTime();

        // 主 SSE 流：thinking + answer
        Flux<ServerSentEvent<String>> mainSse = responseFlux
                .concatMap(response -> {
                    // 思考内容事件
                    ServerSentEvent<String> thinkEvent = buildThinkEvent(response, ctx);
                    // 回复内容事件
                    ServerSentEvent<String> answerEvent = buildAnswerEvent(response, ctx);
                    return Flux.fromIterable(
                            Stream.of(thinkEvent, answerEvent)
                                    .filter(Objects::nonNull)
                                    .toList()
                    );
                })
                .concatWith(
                        Mono.fromSupplier(() -> {
                            long endTime = System.currentTimeMillis();
                            // 元数据事件
                            return buildMetadataEvent(ctx, startTime, endTime);
                        })
                );

        // 工具执行事件 SSE 流（同时累积到上下文用于持久化）
        Flux<ServerSentEvent<String>> toolSse = toolEventFlux
                .map(event -> buildToolExecutionEvent(event, ctx));

        // 合并两个流。
        // 关键：当 mainSse 完成时关闭 Sink，使 toolSse 随之结束，
        // 避免 toolSse 永不完成导致 Flux.merge 无法 complete 的死锁。
        return Flux.merge(
                mainSse.doFinally(signalType -> toolEventSink.tryEmitComplete()),
                toolSse
        );
    }

    /**
     * 构建工具执行 SSE 事件，直接序列化 ToolExecutionEvent 实体
     * 同时累积到上下文用于持久化
     */
    private ServerSentEvent<String> buildToolExecutionEvent(ToolExecutionEvent event, ChatContext ctx) {
        // 累积到上下文（仅 COMPLETE 事件才记录到持久化列表）
        if (ToolExecutionStatus.COMPLETE.equals(event.getStatus())) {
            String toolName = event.getToolName();
            if (toolName != null && toolName.startsWith("built_in_")) {
                String result = event.getResult();
                try {
                    ApiResult<String> apiResult = objectMapper.readValue(result, new TypeReference<ApiResult<String>>() {
                    });
                    if (apiResult.getCode() == ApiResultCode.SUCCESS.getCode()) {
                        event.setResult(apiResult.getData());
                    }
                } catch (JsonProcessingException e) {
                    log.warn("解析内置工具结果失败 -> " + e.getMessage(), e);
                }
            }
            // 工具响应超长时截断（避免数据库存储过大）
            String configValue = globalConfigContext.getConfig(GlobalConfigKey.TOOL_CALL_MAX_LENGTH);
            int maxLength = ConfigUtil.resolveIntConfig(5000, configValue, 100, MAX_TOOL_CALL_RES_LENGTH);
            if (event.getResult() != null && event.getResult().length() > maxLength) {
                event.setResult(event.getResult().substring(0, maxLength) + "...（已截断）");
            }
            ctx.getToolCalls().add(event);
        }
        return ServerSentEvent.<String>builder()
                .event("tool_execution")
                .data(toJson(event))
                .build();
    }

    /**
     * 构建 thinking 事件 — 按 providerCode 匹配 ChatThinkingHandler
     * <p>
     * ChatThinkingHandler 同时负责提取和处理（因为提取逻辑是 provider-specific 的）。
     */
    private ServerSentEvent<String> buildThinkEvent(ChatResponse response, ChatContext ctx) {
        for (ChatThinkingHandler handler : thinkingHandlers) {
            if (handler.support(ctx.getProviderCode())) {
                String thinking = handler.extractThinking(response, ctx);
                if (ObjectUtils.isEmpty(thinking)) {
                    return null;
                }
                // 累加完整思考内容到上下文（用于持久化）
                if (ctx.getFullThinking() == null) {
                    ctx.setFullThinking(new StringBuilder());
                }
                ctx.getFullThinking().append(thinking);
                return ServerSentEvent.<String>builder()
                        .event("thinking")
                        .data(thinking)
                        .build();
            }
        }
        return null;
    }

    /**
     * 构建 answer 事件并累加到 ctx.fullAnswer（用于持久化）
     */
    private ServerSentEvent<String> buildAnswerEvent(ChatResponse response, ChatContext ctx) {
        String content = response.getResults().stream()
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .filter(f -> !ObjectUtils.isEmpty(f))
                .collect(Collectors.joining());
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }
        // 累加完整回答到上下文
        if (ctx.getFullAnswer() == null) {
            ctx.setFullAnswer(new StringBuilder());
        }
        ctx.getFullAnswer().append(content);
        return ServerSentEvent.<String>builder()
                .event("answer")
                .data(content)
                .build();
    }

    /**
     * 构建 metadata 事件
     * <p>
     * 优先使用 ChatMetadataHandler 构建 MetadataInfo 并存入 ctx，
     * 然后直接从 ctx.getMetadata() 读取 MetadataInfo 实体并序列化，
     * 确保 SSE 与持久化数据一致。
     */
    private ServerSentEvent<String> buildMetadataEvent(ChatContext ctx, long startTime, long endTime) {
        long durationMs = endTime - startTime;

        // 优先使用 ChatMetadataHandler
        for (ChatMetadataHandler handler : metadataHandlers) {
            if (handler.support(ctx.getProviderCode())) {
                handler.buildMetadata(ctx);
                MetadataInfo info = ctx.getMetadata();
                if (info != null) {
                    info.setDurationMs(durationMs);
                    info.setTimestamp(endTime);
                    return ServerSentEvent.<String>builder()
                            .event("metadata")
                            .data(toJson(info))
                            .build();
                }
                // info 为 null：回退到空 MetadataInfo
                return ServerSentEvent.<String>builder()
                        .event("metadata")
                        .data(toJson(new MetadataInfo()))
                        .build();
            }
        }

        // fallback：无匹配 handler 时从 ctx 读取 MetadataInfo
        MetadataInfo info = ctx.getMetadata();
        if (info != null) {
            info.setDurationMs(durationMs);
            info.setTimestamp(endTime);
        } else {
            info = MetadataInfo.builder()
                    .durationMs(durationMs)
                    .timestamp(endTime)
                    .build();
        }
        return ServerSentEvent.<String>builder()
                .event("metadata")
                .data(toJson(info))
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
