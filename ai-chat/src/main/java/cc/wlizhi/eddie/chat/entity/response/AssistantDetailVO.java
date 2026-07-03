/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

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

    // 助手偏好设置回显（JSON → Map）
    private java.util.Map<String, Object> preferences;

    // 记忆
    private Integer memoryRounds;

    private Boolean enabled;
    private Integer sortOrder;
    private Long createdAt;
    private Long updatedAt;

    /**
     * 已绑定的 MCP Server ID 列表（回显用）
     */
    private java.util.List<Long> boundMcpServerIds;
}
