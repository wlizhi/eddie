package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 聊天业务实现
 * <p>
 * 使用 DeepSeek 模型（OpenAI 协议兼容），
 * 通过 {@link MessageChatMemoryAdvisor} 自动管理对话记忆。
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private OpenAiChatModel deepSeekChatModel;

    @Resource
    private ChatMemoryManager chatMemoryManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 构建带记忆的 ChatClient
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemoryManager)
                                .build())
                .build();

        // V1：先使用 content() 流式输出回答内容
        // 后续可通过 chatResponse() 获取完整响应，提取 reasoningContent 和 token 统计
        return chatClient.prompt()
                .user(request.getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", request.getConversationId()))
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder()
                        .event("answer")
                        .data(content)
                        .build())
                .concatWithValues(
                        ServerSentEvent.<String>builder()
                                .event("metadata")
                                .data(buildMetadata(startTime))
                                .build()
                );
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
