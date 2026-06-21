package cc.wlizhi.eddieai.chat.entity.request;

import cc.wlizhi.eddieai.chat.entity.dto.ModelParams;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "模型服务商不能为空")
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
     * 记忆轮数，默认 20
     */
    private Integer memoryRounds;
}
