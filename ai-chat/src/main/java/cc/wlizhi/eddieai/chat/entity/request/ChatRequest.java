package cc.wlizhi.eddieai.chat.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 聊天请求参数
 * <p>
 * 第一版硬编码模型参数，后续扩展字段：
 * - providerCode: 供应商 code，从 model_provider 表读取配置
 * - modelId: 具体模型 ID
 * - temperature: 温度参数
 * - systemPrompt: 自定义系统提示词
 */
@Getter
@Setter
public class ChatRequest {

    /**
     * 会话 ID，用于记忆隔离
     */
    @NotBlank(message = "conversationId 不能为空")
    private String conversationId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "message 不能为空")
    private String message;

    /**
     * 供应商 code，仅作分组标识，不可选中
     */
    @NotBlank(message = "providerCode 不能为空")
    private String providerCode;

    /**
     * 模型名称
     */
    @NotBlank(message = "modelId 不能为空")
    private String modelId;
}
