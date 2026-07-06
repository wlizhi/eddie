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
 * 更新智能体请求参数（全量更新，所有必填字段必须传入）
 */
@Getter
@Setter
public class AgentUpdateRequest {

    // ==================== 基本信息 ====================

    /**
     * 智能体名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 功能描述
     */
    private String description;

    /**
     * 系统提示词
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
     * 主模型参数 JSON
     */
    private String mainModelParams;

    // ==================== 子代理模型配置 ====================

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
     * 并发度
     */
    @Min(value = 1, message = "并发度不能小于 1")
    private Integer semaphore;

    /**
     * 最大迭代次数
     */
    @Min(value = 1, message = "最大迭代次数不能小于 1")
    private Integer maxIterations;

    /**
     * 单次执行超时（秒）
     */
    @Min(value = 10, message = "超时时间不能小于 10 秒")
    private Integer maxExecutionTimeSec;

    /**
     * 执行模式：FOREGROUND / BACKGROUND
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
     * 偏好设置 JSON
     */
    private Map<String, Object> preferences;

    // ==================== 状态 & 排序 ====================

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
