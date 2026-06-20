package cc.wlizhi.eddieai.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatClientGetDTO {
    /**
     * 会话 ID，用于记忆隔离
     */
    private String conversationId;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 模型名称
     */
    private String modelId;
}
