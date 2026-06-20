package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.BeanUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        ChatPolicy chatPolicy = getChatPolicy(request);

        ChatClientGetDTO dto = new ChatClientGetDTO();
        BeanUtils.copyProperties(request, dto);
        ChatClient chatClient = chatPolicy.getChatClient(dto);
        // 构建带记忆的 ChatClient
        chatClient = chatClient.mutate()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemoryManager)
                                .build()
                ).build();
        // 获取 ChatResponse 流（包含 reasoning_content 和 content）
        Flux<ChatResponse> responseFlux = chatClient.prompt()
                .system("你是小麦，全名麦泱，一个年轻邻家女孩儿形象，说话甜甜的，让人觉得有点初恋感。你的责任是给用户提供准确的信息，帮助用户处理事务。回复内容要简洁明了、逻辑清晰，语气亲和友好。禁止长篇大论，禁止重复啰嗦、一本正经的胡说八道，禁止暴露你是AI的身份。")
                .user(request.getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", request.getConversationId()))
                .stream()
                .chatResponse();

        // 从 ChatResponse 中分别提取思考内容（reasoning_content）和回答内容（content），
        // 发射对应的 SSE 事件：thinking（由各 ChatPolicy 自定义提取）和 answer
        return responseFlux
                .concatMap(response -> Flux.fromIterable(buildEvents(response, chatPolicy)))
                .concatWithValues(
                        ServerSentEvent.<String>builder()
                                .event("metadata")
                                .data(buildMetadata(startTime))
                                .build()
                );
    }

    private ChatPolicy getChatPolicy(ChatRequest request) {
        for (ChatPolicy policy : chatPolicies) {
            if (policy.support(request.getProviderCode())) {
                return policy;
            }
        }
        return defaultChatPolicy;
    }

    /**
     * 从单个 ChatResponse chunk 中提取 reasoning_content 和 content，
     * 分别构建 thinking 和 answer SSE 事件。
     * <p>
     * thinking 事件由各 ChatPolicy 自定义提取（如 DeepSeek 从 DeepSeekAssistantMessage
     * 中提取 reasoning_content），answer 事件从 AssistantMessage.getText() 统一提取。
     */
    private List<ServerSentEvent<String>> buildEvents(ChatResponse response, ChatPolicy chatPolicy) {
        List<ServerSentEvent<String>> events = new ArrayList<>();

        // 通过 ChatPolicy 提取思考内容（各 provider 实现不同）
        ServerSentEvent<String> thinkEvent = chatPolicy.getThinkEvent(response);
        String thinkData = thinkEvent.data();
        if (thinkData != null && !thinkData.isEmpty()) {
            events.add(thinkEvent);
        }

        // 提取回答内容
        String content = response.getResults().stream()
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining());
        if (!content.isEmpty()) {
            events.add(ServerSentEvent.<String>builder()
                    .event("answer")
                    .data(content)
                    .build());
        }
        return events;
    }

    /**
     * 构建元数据 JSON
     */
    private String buildMetadata(long startTime) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "durationMs", System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
