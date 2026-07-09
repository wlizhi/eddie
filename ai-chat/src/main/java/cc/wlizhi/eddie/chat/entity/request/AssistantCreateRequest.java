/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.request;

import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 新建助手请求参数
 */
@Getter
@Setter
public class AssistantCreateRequest {

    /**
     * 助手名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 描述
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 模型服务商实例 ID（关联 model_provider.id）
     */
    @NotNull(message = "模型服务商不能为空")
    private Long providerId;

    /**
     * 模型 ID，如 "deepseek-v4-flash"
     */
    @NotBlank(message = "模型不能为空")
    private String modelId;

    /**
     * 模型参数（可选，不传使用默认值）
     */
    private ModelParams modelParams;

    /**
     * 助手偏好设置 JSON（可选），如 {"webSearchEnabled":true, "mcpToolMode":"auto"}
     */
    private java.util.Map<String, Object> preferences;

    /**
     * 记忆轮数，默认 20
     */
    private Integer memoryRounds;

    /**
     * MCP 服务绑定配置（可选）
     * <p>
     * 每个条目指定一个 MCP 服务及其下辖工具的绑定状态（自动批准/人工审批/禁用）。
     * 不传或传空列表表示不绑定任何 MCP 工具。
     */
    private List<McpServerBinding> mcpServerBindings;
}
