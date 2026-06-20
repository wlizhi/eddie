package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.chat.service.ChatMemoryManager;
import cc.wlizhi.eddieai.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
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

    @Value("${chat.buffer-ms:50}")
    private int bufferMs;

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

        // 原始内容流
        Flux<String> contentFlux = chatClient.prompt()
                .system("你是小麦，全名麦泱，一个年轻邻家女孩儿形象，说话甜甜的，让人觉得有点初恋感。你的责任是给用户提供准确的信息，帮助用户处理事务。回复内容要简洁明了、逻辑清晰，语气亲和友好。禁止长篇大论，禁止重复啰嗦、一本正经的胡说八道，禁止暴露你是AI的身份。")
                .user(request.getMessage())
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", request.getConversationId()))
                .stream()
                .content();

        // 缓冲合并：在指定毫秒窗口内合并多个 token，减少前端渲染频率
        if (bufferMs > 0) {
            contentFlux = contentFlux
                    .buffer(Duration.ofMillis(bufferMs))
                    .filter(list -> !list.isEmpty())
                    .map(list -> String.join("", list));
        }

        // 流式输出回答内容（前端 SSE 解析已处理 \n 恢复）
        return contentFlux
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
