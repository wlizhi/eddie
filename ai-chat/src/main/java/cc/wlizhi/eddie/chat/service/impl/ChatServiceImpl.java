package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddie.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddie.chat.handler.impl.ChatSseTransformer;
import cc.wlizhi.eddie.chat.handler.impl.ChatStreamExecutor;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.chat.service.ChatClientFactoryRouter;
import cc.wlizhi.eddie.chat.service.ChatService;
import cc.wlizhi.eddie.memory.shortterm.ShortTermMemory;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 聊天业务实现 — 流程编排
 * <p>
 * 编排流程：
 * <ol>
 *   <li>初始化 {@link ChatContext}，解析 sessionId</li>
 *   <li>{@link ChatPreProcessor} 预处理</li>
 *   <li>{@link ChatClientFactoryRouter} 工厂路由 + {@link ChatClientFactory} 构建 ChatClient</li>
 *   <li>注入记忆 Advisor</li>
 *   <li>{@link ChatStreamExecutor} 流式执行</li>
 *   <li>{@link ChatSseTransformer} SSE 事件转换</li>
 *   <li>{@link ChatPostProcessor} 后置处理（消息持久化等）</li>
 * </ol>
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private List<ChatPreProcessor> preProcessors;

    @Resource
    private ChatClientFactoryRouter chatClientFactoryRouter;

    @Resource
    private ShortTermMemory shortTermMemory;

    @Resource
    private ChatStreamExecutor chatStreamExecutor;

    @Resource
    private ChatSseTransformer chatSseTransformer;

    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    @Resource
    private List<ChatPostProcessor> postProcessors;

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        // 1. 初始化上下文
        ChatContext ctx = new ChatContext();
        ctx.setOriginalRequest(request);
        ctx.setStartTime(System.currentTimeMillis());

        // 2. 预处理
        preProcessors.forEach(p -> p.process(ctx));

        // 3. 工厂路由 + 构建 ChatClient
        ChatClientFactory factory = chatClientFactoryRouter.resolve(ctx.getProviderCode());

        // 4. 注入工具 + 记忆 Advisor
        ChatClient chatClient = factory.getChatClient(ctx);
        var builder = chatClient.mutate()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(shortTermMemory)
                                .scheduler(Schedulers.parallel())
                                .build()
                );

        // 4.1 解析工具回调（请求参数优先，其次助手设置）
        ChatRequest chatRequest = ctx.getOriginalRequest();
        String toolMode = chatRequest.getToolSelectionMode();
        if (toolMode == null) {
            toolMode = ctx.getAssistant().getToolSelectionMode();
        }
        Object[] toolCallbacks = toolCallbackResolver.resolve(
                "ASSISTANT", ctx.getAssistant().getId(), toolMode, chatRequest.getToolNames());
        if (toolCallbacks.length > 0) {
            builder.defaultTools(toolCallbacks);
        }

        chatClient = builder.build();
        ctx.setChatClient(chatClient);

        // 5. 执行流式调用
        Flux<ChatResponse> responseFlux = chatStreamExecutor.execute(ctx);

        // 6. SSE 事件转换 + 7. 后置处理
        return chatSseTransformer.transform(responseFlux, ctx)
                .doOnComplete(() -> postProcessors.forEach(p -> p.process(ctx)));
    }
}
