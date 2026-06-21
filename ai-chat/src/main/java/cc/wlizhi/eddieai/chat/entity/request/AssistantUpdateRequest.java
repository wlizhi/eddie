package cc.wlizhi.eddieai.chat.entity.request;

import cc.wlizhi.eddieai.chat.entity.dto.ModelParams;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新助手设置请求参数（支持部分更新）
 */
@Getter
@Setter
public class AssistantUpdateRequest {

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
     * 模型服务商实例 ID
     */
    @NotNull(message = "模型服务商不能为空")
    private Long providerId;

    /**
     * 模型 ID
     */
    @NotBlank(message = "模型不能为空")
    private String modelId;

    /**
     * 模型参数
     */
    private ModelParams modelParams;

    /**
     * 记忆轮数
     */
    private Integer memoryRounds;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
