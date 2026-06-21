package cc.wlizhi.eddieai.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatClientGetDTO {
    /**
     * 会话 ID，用于记忆隔离
     */
    private Long conversationId;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 供应商实例 ID，用于精确查找服务商配置
     */
    private Long providerId;

    /**
     * 模型名称
     */
    private String modelId;
}
