package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.mapper.ChatRequestMapper;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.chat.service.ChatService;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 聊天业务实现
 * <p>
 * 使用 DeepSeek 模型（OpenAI 协议兼容），
 * 通过 {@link MessageChatMemoryAdvisor} 自动管理对话记忆。
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private List<ChatPolicy> chatPolicies;
    @Resource
    private ChatPolicy defaultChatPolicy;

    @Resource
    private ChatMemoryManager chatMemoryManager;
    @Resource
    private ChatRequestMapper chatRequestMapper;
    @Resource
    private ModelProviderContext modelProviderContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        ChatPolicy chatPolicy = getChatPolicy(request);

        ChatClientGetDTO dto = chatRequestMapper.toDto(request);
        ChatClient chatClient = chatPolicy.getChatClient(dto);
        // 构建带记忆的 ChatClient
        chatClient = chatClient.mutate()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemoryManager)
                                .scheduler(Schedulers.parallel())
                                .build()
                ).build();
        // 获取 ChatResponse 流（包含 reasoning_content 和 content）
        AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();
        Flux<ChatResponse> responseFlux = chatClient.prompt()
                .system("你是小麦，全名麦泱，一个年轻邻家女孩儿形象，说话甜甜的，让人觉得有点初恋感。你的责任是给用户提供准确的信息，帮助用户处理事务。回复内容要简洁明了、逻辑清晰，语气亲和友好。禁止长篇大论，禁止重复啰嗦、一本正经的胡说八道，禁止暴露你是AI的身份。")
                .user(request.getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", request.getConversationId()))
                .stream()
                .chatResponse()
                .doOnNext(lastResponseRef::set);

        // 从 ChatResponse 中分别提取思考内容（reasoning_content）和回答内容（content），
        // 发射对应的 SSE 事件：thinking（由各 ChatPolicy 自定义提取）和 answer
        // 最后使用 Mono.fromSupplier 延迟构建 metadata（等流结束后再计算耗时和提取 token 用量）
        return responseFlux
                .concatMap(response -> Flux.fromIterable(
                        Stream.of(chatPolicy.getThinkEvent(response), getContentEvent(response))
                                .filter(Objects::nonNull)
                                .toList()
                )).concatWith(
                        Mono.fromSupplier(() -> {
                            long endTime = System.currentTimeMillis();
                            return ServerSentEvent.<String>builder()
                                    .event("metadata")
                                    .data(buildMetadata(startTime, endTime, lastResponseRef.get()))
                                    .build();
                        })
                );
    }

    private ChatPolicy getChatPolicy(ChatRequest request) {
        if (request.getProviderId() == null) {
            throw new BadRequestException("providerId 不能为空");
        }
        ModelProviderEntity provider = modelProviderContext.getModelProviderById(request.getProviderId());
        if (provider == null) {
            throw new BadRequestException("providerId=" + request.getProviderId() + " 不存在的模型服务商");
        }
        for (ChatPolicy policy : chatPolicies) {
            if (policy.support(provider.getCode())) {
                return policy;
            }
        }
        return defaultChatPolicy;
    }

    private ServerSentEvent<String> getContentEvent(ChatResponse response) {
        // 提取回答内容
        String content = response.getResults().stream()
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .filter(f -> !ObjectUtils.isEmpty(f))
                .collect(Collectors.joining());
        return ObjectUtils.isEmpty(content) ? null : ServerSentEvent.<String>builder()
                .event("answer")
                .data(content)
                .build();
    }

    /**
     * 构建元数据 JSON（含 token 用量、时间戳）
     *
     * @param startTime  请求开始时间戳（毫秒）
     * @param endTime    请求结束时间戳（毫秒）
     * @param lastResponse 最后一条 ChatResponse（用于提取 token 用量等信息）
     */
    private String buildMetadata(long startTime, long endTime, ChatResponse lastResponse) {
        try {
            long durationMs = endTime - startTime;

            // 从最后一次 ChatResponse 中提取 token 用量
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;
            if (lastResponse != null) {
                ChatResponseMetadata metadata = lastResponse.getMetadata();
                Usage usage = metadata.getUsage();
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
                totalTokens = usage.getTotalTokens();
            }

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("durationMs", durationMs);
            data.put("timestamp", endTime);
            if (promptTokens != null) data.put("promptTokens", promptTokens);
            if (completionTokens != null) data.put("completionTokens", completionTokens);
            if (totalTokens != null) data.put("totalTokens", totalTokens);

            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
