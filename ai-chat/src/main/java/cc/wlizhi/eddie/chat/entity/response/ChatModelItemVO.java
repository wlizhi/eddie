package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 聊天模型选择器中的单个模型项
 */
@Getter
@Setter
public class ChatModelItemVO {

    /**
     * 模型 ID，如 "deepseek-v4-flash"，选中时提交的值
     */
    private String modelId;

    /**
     * 模型显示名，为 null 时前端 fallback 到 modelId
     */
    private String displayName;

    /**
     * 所属供应商实例 ID
     */
    private Long providerId;

    /**
     * 模型提供商代码，如 "deepseek"
     */
    private String providerCode;
}
