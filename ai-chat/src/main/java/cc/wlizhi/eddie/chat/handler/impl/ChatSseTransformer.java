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

/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecPayload;
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
    private static final int MAX_TOOL_CALL_RES_LENGTH = 20000;


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
                            // 如果被中断，先发送 cancelled 事件
                            if (ctx.isInterrupted()) {
                                String reason = (String) ctx.getAttributes().get("cancelMode");
                                if (reason == null) reason = "unknown";
                                return buildCancelledEvent(reason);
                            }
                            // 元数据事件
                            return buildMetadataEvent(ctx, startTime, endTime);
                        })
                ).onErrorResume(e -> {
                    log.error("Chat 流式响应异常: {}", e.getMessage(), e);
                    String message = extractFriendlyErrorMessage(e);
                    String data = "{\"message\":\"" + escapeJson(message) + "\"}";
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(data)
                            .build());
                });

        // 工具执行事件 SSE 流（同时累积到上下文用于持久化）
        Flux<ServerSentEvent<String>> toolSse = toolEventFlux
                .map(event -> buildToolExecutionEvent(event, ctx));

        // 合并两个流。
        // 关键：当 mainSse 完成时关闭 Sink，使 toolSse 随之结束，
        // 避免 toolSse 永不完成导致 Flux.merge 无法 complete 的死锁。
        return Flux.merge(
                mainSse.doFinally(signalType -> toolEventSink.tryEmitComplete()),
                toolSse
        ).doOnCancel(() -> {
            log.debug("SSE 流被用户中断");
            ctx.setInterrupted(true);
            toolEventSink.tryEmitComplete();
        });
    }

    /**
     * 构建工具执行 SSE 事件，包装为 ApiResult&lt;ChatToolExecPayload&gt; 信封。
     * PENDING_APPROVAL 事件直接透传（不累积到上下文），
     * COMPLETE 事件同时累积到上下文用于持久化。
     */
    private ServerSentEvent<String> buildToolExecutionEvent(ToolExecutionEvent event, ChatContext ctx) {
        // 构建公共 Payload（统一携带 msgId，用于审批场景）
        ChatToolExecPayload payload = new ChatToolExecPayload(
                ctx.getPlaceholderMsgId(),
                event.getToolName(),
                event.getStatus().getValue(),
                event.getArguments(),
                event.getResult(),
                event.isError(),
                event.getSeq()
        );

        // PENDING_APPROVAL 事件直接透传，不累积到上下文
        if (ToolExecutionStatus.PENDING_APPROVAL.equals(event.getStatus())) {
            return buildSseEvent(payload);
        }

        // 累积到上下文（仅 COMPLETE / REJECTED 事件记录到持久化列表，START 事件不持久化）
        if (event.getStatus() == ToolExecutionStatus.COMPLETE
                || event.getStatus() == ToolExecutionStatus.REJECTED) {
            // 尝试解包内置工具的 ApiResult 包裹，非 ApiResult 格式（如 MCP 工具结果）直接忽略
            if (event.getStatus() == ToolExecutionStatus.COMPLETE) {
                String result = event.getResult();
                if (result != null) {
                    try {
                        ApiResult<String> apiResult = objectMapper.readValue(result, new TypeReference<ApiResult<String>>() {
                        });
                        if (apiResult.getCode() == ApiResultCode.SUCCESS.getCode()) {
                            event.setResult(apiResult.getData());
                        }
                    } catch (JsonProcessingException e) {
                        // 不是 ApiResult 格式（如 MCP 工具结果），忽略
                    }
                }
            }
            // 第一阶段截断：SSE 实时渲染，允许更大的值
            String configValue = globalConfigContext.getConfig(GlobalConfigKey.TOOL_CALL_MAX_LENGTH);
            int sseMaxLength = ConfigUtil.resolveIntConfig(10000, configValue, 100, MAX_TOOL_CALL_RES_LENGTH);
            if (event.getResult() != null && event.getResult().length() > sseMaxLength) {
                event.setResult(event.getResult().substring(0, sseMaxLength) + "...（已截断）");
            }
            // 更新 payload 中的 result 为截断后的值
            payload.setResult(event.getResult());

            // 第二阶段截断：保存到上下文/入库时使用更小的边界（MAX_TOOL_CALL_RES_LENGTH >> 1）
            int storeMaxLength = MAX_TOOL_CALL_RES_LENGTH >> 1;
            if (event.getResult() != null && event.getResult().length() > storeMaxLength) {
                event.setResult(event.getResult().substring(0, storeMaxLength) + "...（已截断）");
            }
            ctx.getToolCalls().add(event);
        }

        return buildSseEvent(payload);
    }

    /**
     * 将 ChatToolExecPayload 构建为 tool_execution SSE 事件（ApiResult 信封格式）
     */
    private ServerSentEvent<String> buildSseEvent(ChatToolExecPayload payload) {
        return ServerSentEvent.<String>builder()
                .event("tool_execution")
                .data(toJson(ApiResult.success(payload)))
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

    /**
     * 构建 cancelled 事件 — 用户停止回答时发送
     */
    private ServerSentEvent<String> buildCancelledEvent(String reason) {
        return ServerSentEvent.<String>builder()
                .event("cancelled")
                .data("{\"reason\":\"" + reason + "\"}")
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化 SSE 事件失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 提取友好错误提示 — 遍历异常链，根据异常类名和消息返回中文友好提示
     * <p>
     * 优先按异常类名精确匹配遍历异常链，429 状态码仅在遍历完所有异常、没有类名匹配成功时兜底使用，
     * 避免 CompletionException 的 message 包含 "429" 导致提前返回、错过内层 RateLimitException 的判断。
     */
    private static String extractFriendlyErrorMessage(Throwable e) {
        Throwable cause = e;
        boolean has429 = false;
        while (cause != null) {
            String clsName = cause.getClass().getName();
            String msg = cause.getMessage() != null ? cause.getMessage() : "";
            if (clsName.contains("RateLimitException")) {
                if (msg.contains("quota")) {
                    return "API 配额不足，请检查账户余额及套餐";
                }
                // 其他 RateLimitException（如频率限制）在遍历结束后兜底处理
            }
            if (clsName.contains("AuthenticationException") || msg.contains("401")) {
                return "API 认证失败，请检查 API Key 是否正确";
            }
            if (msg.contains("429")) {
                has429 = true;
            }
            cause = cause.getCause();
        }
        // 遍历完异常链后，根据收集的信息兜底
        if (has429) {
            return "API 配额不足或请求过于频繁，请检查账户余额及套餐";
        }
        // 兜底：返回原始消息（短）或通用提示（长）
        String originalMsg = e.getMessage();
        if (originalMsg != null && originalMsg.length() < 200) {
            return originalMsg;
        }
        return "服务暂不可用，请稍后重试";
    }

    /**
     * 对 JSON 字符串值中的特殊字符转义
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
