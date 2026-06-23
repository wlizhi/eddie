package cc.wlizhi.eddie.chat.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 聊天请求参数
 * <p>
 * - providerId: 供应商实例 ID，用于精确查找服务商配置
 * - modelId: 具体模型 ID
 */
@Getter
@Setter
public class ChatRequest {

    /**
     * 会话 ID，用于记忆隔离
     */
    @NotNull(message = "conversationId 不能为空")
    private Long conversationId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "message 不能为空")
    private String message;

    /**
     * 供应商实例 ID，用于精确查找服务商配置
     */
    private Long providerId;

    /**
     * 模型名称
     */
    @NotBlank(message = "modelId 不能为空")
    private String modelId;
}
