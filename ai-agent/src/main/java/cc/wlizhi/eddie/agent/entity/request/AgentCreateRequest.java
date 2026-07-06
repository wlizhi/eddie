/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 新建智能体请求参数
 * <p>
 * 智能体比普通助手多了：主/子模型配置、执行控制参数、工具选择模式等。
 */
@Getter
@Setter
public class AgentCreateRequest {

    // ==================== 基本信息 ====================

    /**
     * 智能体名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 头像（文字/emoji/图片 URL，可选）
     */
    private String avatar;

    /**
     * 功能描述
     */
    private String description;

    /**
     * 系统提示词（任务指令）
     */
    private String systemPrompt;

    // ==================== 主模型配置 ====================

    /**
     * 主模型服务商实例 ID
     */
    @NotNull(message = "主模型服务商不能为空")
    private Long mainProviderId;

    /**
     * 主模型 ID
     */
    @NotBlank(message = "主模型不能为空")
    private String mainModelId;

    /**
     * 主模型参数 JSON（可选）
     */
    private String mainModelParams;

    // ==================== 子代理模型配置（可选） ====================

    /**
     * 子代理模型服务商实例 ID
     */
    private Long subProviderId;

    /**
     * 子代理模型 ID
     */
    private String subModelId;

    /**
     * 子代理模型参数 JSON
     */
    private String subModelParams;

    // ==================== 执行控制 ====================

    /**
     * 并发度，默认 1
     */
    @Min(value = 1, message = "并发度不能小于 1")
    private Integer semaphore;

    /**
     * 最大迭代次数，默认 100
     */
    @Min(value = 1, message = "最大迭代次数不能小于 1")
    private Integer maxIterations;

    /**
     * 单次执行超时（秒），默认 300
     */
    @Min(value = 10, message = "超时时间不能小于 10 秒")
    private Integer maxExecutionTimeSec;

    /**
     * 执行模式：FOREGROUND / BACKGROUND，默认 FOREGROUND
     */
    private String executionMode;

    // ==================== 记忆轮数 ====================

    /**
     * 记忆轮数，默认 20
     */
    private Integer memoryRounds;

    // ==================== 工具选择 ====================

    /**
     * 工具选择模式：auto / manual / none
     */
    private String toolSelectionMode;

    /**
     * 已启用的 MCP Server ID 列表
     */
    private List<Long> enabledMcpServerIds;

    // ==================== 偏好设置 ====================

    /**
     * 偏好设置 JSON，如 {"webSearchEnabled":true}
     */
    private Map<String, Object> preferences;
}
