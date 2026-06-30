/**
 * @author Eddie
 * {@code @date} 2026-06-24
 */

package cc.wlizhi.eddie.memory.shortterm;

import org.springframework.ai.chat.memory.ChatMemory;

/**
 * 短期记忆接口 — 扩展 Spring AI 的 {@link ChatMemory}
 * <p>
 * 在 {@link ChatMemory} 的基础上增加获取会话记忆轮数的能力，
 * 各业务模块（助手、智能体等）提供各自的实现。
 * <p>
 * <b>实现类职责：</b>
 * <ul>
 *   <li>管理基于窗口的短期记忆（LRU + TTL + 动态轮次）</li>
 *   <li>根据 conversationId 解析该会话的 {@code memoryRounds} 配置</li>
 *   <li>超出轮次限制自动裁剪最早消息</li>
 *   <li>全局会话数上限 + LRU 淘汰 + 超时懒淘汰</li>
 * </ul>
 */
public interface ShortTermMemory extends ChatMemory {

    /**
     * 获取指定会话的记忆轮数
     *
     * @param conversationId 会话 ID
     * @return 配置的记忆轮数；{@code <=0} 表示不限制
     */
    int getRoundsForConversation(String conversationId);
}
