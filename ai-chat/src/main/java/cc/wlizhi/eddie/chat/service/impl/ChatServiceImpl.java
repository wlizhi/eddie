/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.advisor.ModelThrottleAdvisor;
import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatErrorPayload;
import cc.wlizhi.eddie.chat.entity.dto.ChatMessageCreatedPayload;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.enums.ChatSseEvent;
import cc.wlizhi.eddie.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddie.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddie.chat.handler.impl.ChatSseTransformer;
import cc.wlizhi.eddie.chat.handler.impl.ChatStreamExecutor;
import cc.wlizhi.eddie.chat.handler.impl.UnifiedChatToolInterceptor;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.chat.service.ChatClientFactoryRouter;
import cc.wlizhi.eddie.chat.service.ChatService;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.cache.SessionLockManager;
import cc.wlizhi.eddie.common.dao.MessageDao;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.entity.MessageEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天业务实现 — 流程编排
 * <p>
 * 编排流程：
 * <ol>
 *   <li>初始化 {@link ChatContext}，解析 sessionId</li>
 *   <li>{@link ChatPreProcessor} 预处理</li>
 *   <li>会话级互斥锁 — 同一会话同时只允许一个请求</li>
 *   <li>事务性持久化 user 消息 + 占位 assistant 消息</li>
 *   <li>{@link ChatClientFactoryRouter} 工厂路由 + {@link ChatClientFactory} 构建 ChatClient</li>
 *   <li>注入记忆 Advisor + 工具</li>
 *   <li>{@link ChatStreamExecutor} 流式执行</li>
 *   <li>{@link ChatSseTransformer} SSE 事件转换（含工具执行事件 + 中断标记）</li>
 *   <li>{@link ChatPostProcessor} 后置处理 — 更新占位消息为实际内容</li>
 * </ol>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Resource
    private List<ChatPreProcessor> preProcessors;

    @Resource
    private ChatClientFactoryRouter chatClientFactoryRouter;

    @Resource
    private AssistantShortTermMemory shortTermMemory;

    @Resource
    private ChatStreamExecutor chatStreamExecutor;

    @Resource
    private ChatSseTransformer chatSseTransformer;

    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    @Resource
    private List<ChatPostProcessor> postProcessors;

    @Resource
    private SessionLockManager sessionLockManager;

    @Resource
    private MessageDao messageDao;

    @Resource
    private SessionDao sessionDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private EventRegistry chatEventRegistry;

    @Resource
    private ModelThrottleAdvisor modelThrottleAdvisor;

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        // 1. 初始化上下文
        ChatContext ctx = new ChatContext();
        ctx.setOriginalRequest(request);
        ctx.getFlowState().setStartTime(System.currentTimeMillis());

        // 2. 预处理（含 session 解析、助手解析等）
        preProcessors.forEach(p -> p.process(ctx));

        // 3. 会话级互斥锁 — 避免同一会话并发请求
        Long sessionId = ctx.getSession().getId();
        long lockToken = sessionLockManager.tryLock(sessionId);
        if (lockToken == 0L) {
            log.warn("会话 {} 正在处理中，拒绝并发请求", sessionId);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event(ChatSseEvent.ERROR.getEventName())
                    .data(toJson(new ChatErrorPayload("该会话正在处理中，请等待当前回答完成")))
                    .build());
        }
        ctx.getFlowState().setLockNanoTime(lockToken);

        try {
            return doChat(ctx);
        } catch (Exception e) {
            sessionLockManager.unlock(sessionId, ctx.getFlowState().getLockNanoTime());
            log.error("聊天请求处理异常", e);
            throw e;
        }
    }

    @Override
    public void stop(String userMessageId, String mode) {
        String key = EventRegistry.key(ChatSseEvent.STOP.getEventName(), userMessageId);
        log.info("用户{}停止回答: userMessageId={}", "forced".equals(mode) ? "强制" : "优雅", userMessageId);
        chatEventRegistry.register(key, mode);
    }

    /**
     * 实际聊天执行流程（持有 session 锁）
     */
    private Flux<ServerSentEvent<String>> doChat(ChatContext ctx) {
        Long sessionId = ctx.getSession().getId();

        // 4. 事务性持久化 user 消息 + 占位 assistant 消息（单事务保证原子性）
        persistInitialMessages(ctx);

        // 4.5 持久化完成后立即创建 message_created 事件（不放后面等准备工作）
        //     告知前端用户消息 ID，用于停止事件关联。事件在 concatWith 中先于流式内容发射
        ChatMessageCreatedPayload chatMessageCreatedPayload = new ChatMessageCreatedPayload(
                ctx.getUserMsgContext().getMsgId(),
                ctx.getAssistantMsgContext().getAssistantMsgId()
        );
        Flux<ServerSentEvent<String>> messageCreatedEvent = Flux.just(ServerSentEvent.<String>builder()
                .event(ChatSseEvent.MESSAGE_CREATED.getEventName())
                .data(toJson(chatMessageCreatedPayload))
                .build());

        // 5. 工厂路由 + 构建 ChatClient
        ChatClientFactory factory = chatClientFactoryRouter.resolve(ctx.getProvider().getCode());

        // 6. 注入节流 Advisor + 工具 + 记忆 Advisor
        ChatClient chatClient = factory.getChatClient(ctx);
        var builder = chatClient.mutate()
                .defaultAdvisors(
                        modelThrottleAdvisor,
                        MessageChatMemoryAdvisor.builder(shortTermMemory)
                                .scheduler(Schedulers.parallel())
                                .build()
                );

        // 6.1 解析工具回调（请求参数优先，其次助手设置）
        ChatRequest chatRequest = ctx.getOriginalRequest();
        String toolMode = chatRequest.getToolSelectionMode();
        if (toolMode == null) {
            toolMode = ctx.getAssistant().getToolSelectionMode();
        }
        ToolCallback[] toolCallbacks = toolCallbackResolver.resolve(
                RoleType.ASSISTANT.name(), ctx.getAssistant().getId(), toolMode, chatRequest.getToolNames());

        // 6.2 创建工具执行事件 Sinks（旁路通道，零 token 消耗）
        Sinks.Many<ChatToolExecutionEvent> toolEventSink = Sinks.many().unicast().onBackpressureBuffer();

        // 6.2.5 一步完成：用 UnifiedChatToolInterceptor 包装所有工具回调
        //       内置三层审批决策（助手级 enabled → 行为匹配 → 用户配置），无 instanceof 判断
        List<ToolDefinitionEntity> boundTools = ownerToolBindingContext.getBoundTools(
                RoleType.ASSISTANT.name(), ctx.getAssistant().getId());
        Map<String, ToolDefinitionEntity> toolDefMap = boundTools.stream()
                .collect(Collectors.toMap(ToolDefinitionEntity::getName, t -> t, (a, b) -> a));
        Map<String, List<ToolBehavior>> behaviorMap = toolCallbackResolver.getBehaviorMap();

        if (toolCallbacks.length > 0) {
            ToolCallback[] wrappedCallbacks = Arrays.stream(toolCallbacks)
                    .map(tc -> {
                        String name = tc.getToolDefinition().name();
                        ToolDefinitionEntity def = toolDefMap.get(name);
                        List<ToolBehavior> behaviors = behaviorMap.getOrDefault(name, List.of());
                        McpServerEntity mcpServer = def != null && def.getMcpServerId() != null
                                ? ownerToolBindingContext.getMcpServer(def.getMcpServerId())
                                : null;
                        return new UnifiedChatToolInterceptor(
                                tc, def, behaviors, mcpServer,
                                chatEventRegistry, ctx.getAssistantMsgContext().getAssistantMsgId(),
                                toolEventSink, ctx.getFlowState().getToolCallSequence());
                    })
                    .toArray(ToolCallback[]::new);
            builder.defaultTools((Object[]) wrappedCallbacks);
        }
        Flux<ChatToolExecutionEvent> toolEventFlux = toolEventSink.asFlux();

        chatClient = builder.build();
        ctx.setChatClient(chatClient);

        // 7. 执行流式调用
        Flux<ChatResponse> responseFlux = chatStreamExecutor.execute(ctx);

        // 8. SSE 事件转换（合并工具执行事件）
        // 9. doFinally：无论 complete / error / cancel 都执行后置处理（更新占位消息）
        //     注意：锁在 post-processing 之前释放，避免其他请求等待 DB 写入
        //     同时清理注册表中的停止事件
        String registryKey = EventRegistry.key(ChatSseEvent.STOP.getEventName(), String.valueOf(ctx.getAssistantMsgContext().getAssistantMsgId()));
        return messageCreatedEvent.concatWith(
                chatSseTransformer.transform(responseFlux, ctx, toolEventFlux, toolEventSink)
        ).doFinally(signalType -> {
                    // 第一步：立即释放锁 + 清理注册表（不阻塞其他请求）
                    chatEventRegistry.remove(registryKey);
                    sessionLockManager.unlock(sessionId, ctx.getFlowState().getLockNanoTime());

                    // 第二步：后置处理（DB 写入，不再持有锁）
                    try {
                        postProcessors.forEach(p -> p.process(ctx));
                    } catch (Exception e) {
                        log.error("后置处理异常", e);
                    }
                });
    }

    /**
     * 创建一个预设默认零值的空壳 MessageEntity
     * <p>
     * 调用方按需覆盖 role / content / msgStatus / sessionId / assistantId / providerId / modelCode / modelName 等字段。
     */
    private static MessageEntity createEmptyMessage() {
        MessageEntity msg = new MessageEntity();
        msg.setThinking("");
        msg.setPromptTokens(0);
        msg.setCompletionTokens(0);
        msg.setTotalTokens(0);
        msg.setCacheReadInputTokens(0);
        msg.setCacheWriteInputTokens(0);
        msg.setPriceEstimate(0.0);
        msg.setToolCalls("[]");
        msg.setRoundSeq(0L);
        return msg;
    }

    /**
     * 将对象序列化为 JSON 字符串，序列化失败返回 "{}"
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化 SSE 事件失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 事务性持久化：user 消息 + 占位 assistant 消息
     * <p>
     * 保证 user 和占位 assistant 始终成对写入，不会出现孤立数据。
     * 使用 TransactionTemplate 而非 @Transactional 避免自调用代理失效问题。
     */
    protected void persistInitialMessages(ChatContext ctx) {
        transactionTemplate.execute(status -> {
            Long sessionId = ctx.getSession().getId();
            Long assistantId = ctx.getSession().getAssistantId();
            ChatRequest request = ctx.getOriginalRequest();

            // user 消息（round_seq=0，插入后回填为自己的 ID）
            MessageEntity userMsg = createEmptyMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setAssistantId(assistantId);
            userMsg.setRole("user");
            userMsg.setContent(ctx.getUserMsgContext().getContent());
            userMsg.setProviderId(request.getProviderId());
            userMsg.setModelCode(request.getModelId());
            userMsg.setModelName(request.getModelId());
            userMsg.setMsgStatus("COMPLETED");
            Long userMsgId = messageDao.insert(userMsg);
            ctx.getUserMsgContext().setMsgId(userMsgId);

            // 占位 assistant 消息（round_seq=0，status=STREAMING，流结束后更新）
            MessageEntity assistantMsg = createEmptyMessage();
            assistantMsg.setSessionId(sessionId);
            assistantMsg.setAssistantId(assistantId);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent("");
            assistantMsg.setProviderId(request.getProviderId());
            assistantMsg.setModelCode(request.getModelId());
            assistantMsg.setModelName(request.getModelId());
            assistantMsg.setMsgStatus("STREAMING");
            assistantMsg.setDurationMs(0);
            Long placeholderId = messageDao.insert(assistantMsg);
            ctx.getAssistantMsgContext().setAssistantMsgId(placeholderId);

            // 更新会话计数器（+2 表示 user + assistant）
            sessionDao.touchAndIncrementMessageCount(sessionId, 2, 0);
            return null;
        });
    }
}
