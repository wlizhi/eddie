/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.response;

import cc.wlizhi.eddie.chat.entity.request.McpServerBinding;
import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
     * MCP 服务绑定配置（含工具级状态，回显用）
     * <p>
     * 每个条目包含 MCP 服务 ID 及其下辖工具的绑定状态（0=禁用, 1=自动批准, 2=人工审批）。
     */
    private List<McpServerBinding> mcpServerBindings;
}
