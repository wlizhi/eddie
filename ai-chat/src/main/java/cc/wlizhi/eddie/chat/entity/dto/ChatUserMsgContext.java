/**
 * ChatUserMsgContext — 用户消息上下文
 * <p>
 * 贯穿聊天调用链路，承载用户消息原文和持久化后的 ID。
 * 与 {@link ChatAssistantMsgContext} 对称设计，避免在 {@link ChatContext} 中散落零散字段。
 */

/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatUserMsgContext {
    /**
     * 持久化后的用户消息 ID（由 MessageDao.insert() 返回）
     */
    private Long msgId;

    /**
     * 用户消息原文
     */
    private String content;
}
