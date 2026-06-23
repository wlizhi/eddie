package cc.wlizhi.eddie.chat.entity.response;

import cc.wlizhi.eddie.chat.entity.dto.ModelParams;
import lombok.Getter;
import lombok.Setter;

/**
 * 助手详情 VO（配置回显）
 */
@Getter
@Setter
public class AssistantDetailVO {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private String systemPrompt;

    // 模型选择回显
    private Long providerId;
    private String providerCode;
    private String providerName;
    private String modelId;

    // 模型参数回显
    private ModelParams modelParams;

    // 记忆
    private Integer memoryRounds;

    private Boolean enabled;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
}
