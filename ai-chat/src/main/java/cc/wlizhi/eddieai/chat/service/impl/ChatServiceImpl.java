package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.handler.PreProcessor;
import cc.wlizhi.eddieai.chat.handler.impl.ChatExecutionStage;
import cc.wlizhi.eddieai.chat.handler.impl.SseEventTransformer;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.chat.service.ChatPolicyRouter;
import cc.wlizhi.eddieai.chat.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 聊天业务实现 — 流程编排
 * <p>
 * 职责：按 Pipeline 模式编排聊天请求的完整处理流程。
 * 每个阶段由独立的组件处理，通过 {@link ChatContext} 传递数据。
 * <p>
 * 编排流程：
 * <ol>
 *   <li>初始化 {@link ChatContext}</li>
 *   <li>{@link PreProcessor} 预处理（校验、查 Provider、获取 SystemPrompt）</li>
 *   <li>{@link ChatPolicyRouter} 策略路由</li>
 *   <li>构建 {@link ChatClient} + 注入记忆</li>
 *   <li>{@link ChatExecutionStage} 流式执行</li>
 *   <li>{@link SseEventTransformer} SSE 事件转换</li>
 * </ol>
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private List<PreProcessor> preProcessors;

    @Resource
    private ChatPolicyRouter chatPolicyRouter;

    @Resource
    private ChatMemoryManager chatMemoryManager;

    @Resource
    private ChatExecutionStage chatExecutionStage;

    @Resource
    private SseEventTransformer sseEventTransformer;

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        // 1. 初始化上下文
        ChatContext ctx = new ChatContext();
        ctx.setOriginalRequest(request);
        ctx.setStartTime(System.currentTimeMillis());

        // 2. 预处理（校验、查 Provider、获取 SystemPrompt 等）
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
        Flux<org.springframework.ai.chat.model.ChatResponse> responseFlux =
                chatExecutionStage.execute(ctx);

        // 6. SSE 事件转换（thinking / answer / metadata）
        return sseEventTransformer.transform(responseFlux, ctx);
    }
}
