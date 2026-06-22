package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.handler.ChatPostProcessor;
import cc.wlizhi.eddieai.chat.handler.ChatPreProcessor;
import cc.wlizhi.eddieai.chat.handler.impl.ChatSseTransformer;
import cc.wlizhi.eddieai.chat.handler.impl.ChatStreamExecutor;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.chat.service.ChatPolicyRouter;
import cc.wlizhi.eddieai.chat.service.ChatService;
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
 *   <li>{@link ChatPolicyRouter} 策略路由</li>
 *   <li>构建 {@link ChatClient} + 注入记忆</li>
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
    private ChatPolicyRouter chatPolicyRouter;

    @Resource
    private ChatMemoryManager chatMemoryManager;

    @Resource
    private ChatStreamExecutor chatStreamExecutor;

    @Resource
    private ChatSseTransformer chatSseTransformer;

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

        // 3. 策略路由
        ChatPolicy chatPolicy = chatPolicyRouter.resolve(ctx.getProviderCode());
        ctx.setChatPolicy(chatPolicy);

        // 4. 构建 ChatClient + 注入记忆 Advisor
        ChatClient chatClient = chatPolicy.getChatClient(ctx.getChatClientGetDTO());
        chatClient = chatClient.mutate()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemoryManager)
                                .scheduler(Schedulers.parallel())
                                .build()
                ).build();
        ctx.setChatClient(chatClient);

        // 5. 执行流式调用
        Flux<ChatResponse> responseFlux = chatStreamExecutor.execute(ctx);

        // 6. SSE 事件转换 + 7. 后置处理
        return chatSseTransformer.transform(responseFlux, ctx)
                .doOnComplete(() -> postProcessors.forEach(p -> p.process(ctx)));
    }
}
