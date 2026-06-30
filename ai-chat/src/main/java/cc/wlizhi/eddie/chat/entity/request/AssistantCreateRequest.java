/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.request;

import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
     * 启用的 MCP Server ID 列表（可选）
     * <p>
     * 传入后自动为该助手绑定这些 MCP Server 下的所有工具。
     */
    private java.util.List<Long> enabledMcpServerIds;
}
