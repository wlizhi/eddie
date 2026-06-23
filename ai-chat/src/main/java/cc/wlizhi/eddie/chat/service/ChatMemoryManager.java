package cc.wlizhi.eddie.chat.service;

import org.springframework.ai.chat.memory.ChatMemory;

/**
 * 聊天记忆管理器
 * <p>
 * 统一管理短期、中期、长期三层记忆结构。
 * <p>
 * <b>第一版实现：</b>仅短期内存记忆，使用 {@link ChatMemory} 接口，
 * 按 conversationId 隔离，自动控制记忆轮次上限。
 * <p>
 * <b>后续扩展：</b>
 * <ul>
 *   <li>中期压缩：超出轮次上限后调用 LLM 压缩，保留关键信息</li>
 *   <li>长期摘要：会话结束后聚合中期记忆，生成跨会话摘要</li>
 * </ul>
 */
public interface ChatMemoryManager extends ChatMemory {

    /**
     * 获取默认记忆轮次上限
     */
    int getMaxRounds();
}
