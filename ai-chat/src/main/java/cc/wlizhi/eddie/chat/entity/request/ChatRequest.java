package cc.wlizhi.eddie.chat.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 聊天请求参数
 * <p>
 * - providerId: 供应商实例 ID，用于精确查找服务商配置
 * - modelId: 具体模型 ID
 * - toolSelectionMode: 工具选择模式，为空则使用助手的设置（最高优先级）
 * - toolNames: 手动模式下指定的工具名称列表
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

    /**
     * 工具选择模式（auto / manual / none）
     * <p>
     * 优先级高于助手的 tool_selection_mode 设置。
     * 前端在聊天输入框底部临时设置，仅当前会话生效，不持久化。
     * 为空时使用助手的设置。
     */
    private String toolSelectionMode;

    /**
     * 手动模式下指定的工具名称列表
     * <p>
     * 仅当 toolSelectionMode=manual 时有效。
     * 列表为空表示不使用任何工具。
     * 勾选的工具必须是助手设置中已启用的工具。
     */
    private List<String> toolNames;

    /**
     * 思考模式：auto / low / medium / high / max / disabled
     * <p>
     * - auto: 不传递参数，让模型自己决定
     * - low/medium/high: 对应 reasoning_effort 参数
     * - max: 最大思考力度（DeepSeek 特有）
     * - disabled: 禁用思考
     * <p>
     * 优先级高于助手的 modelParams 中的配置。
     */
    private String thinkingMode;
}
