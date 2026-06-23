package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.service.ChatMemoryManager;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天记忆管理器实现（纯内存版）
 * <p>
 * 第一版：仅使用 {@link MessageWindowChatMemory}，按 conversationId 隔离，
 * 超出 {@link #MAX_ROUNDS} 轮次自动丢弃最早消息。
 * <p>
 * <b>后续扩展：</b>
 * <ol>
 *   <li>短期 -&gt; 追加 DB 持久化（全量历史）</li>
 *   <li>中期 -&gt; LLM 压缩摘要，超出阈值时触发</li>
 *   <li>长期 -&gt; 跨会话长期摘要归档</li>
 * </ol>
 */
@Service
public class ChatMemoryManagerImpl implements ChatMemoryManager {

    /**
     * 默认保留最近 20 轮对话
     */
    private static final int MAX_ROUNDS = 20;

    /**
     * Spring AI 原生内存记忆，自动管理轮次上限
     */
    private final ChatMemory delegate = MessageWindowChatMemory.builder()
            .maxMessages(MAX_ROUNDS)
            .build();

    @Override
    public int getMaxRounds() {
        return MAX_ROUNDS;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
