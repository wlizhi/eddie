/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

import cc.wlizhi.eddie.agent.entity.request.AgentMcpServerBinding;

/**
 * 智能体详情 VO（配置回显）
 */
@Getter
@Setter
public class AgentDetailVO {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private String systemPrompt;

    // ==================== 主模型配置 ====================

    /**
     * 主模型服务商实例 ID
     */
    private Long mainProviderId;
    /**
     * 主模型服务商 code
     */
    private String mainProviderCode;
    /**
     * 主模型服务商名称
     */
    private String mainProviderName;
    /**
     * 主模型 ID
     */
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
     * 子代理模型服务商 code
     */
    private String subProviderCode;
    /**
     * 子代理模型服务商名称
     */
    private String subProviderName;
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
    private Integer semaphore;
    /**
     * 最大迭代次数
     */
    private Integer maxIterations;
    /**
     * 单次执行超时（秒）
     */
    private Integer maxExecutionTimeSec;
    /**
     * 执行模式
     */
    private String executionMode;

    // ==================== 记忆轮数 ====================

    /**
     * 记忆轮数
     */
    private Integer memoryRounds;

    // ==================== 工具选择 ====================

    /**
     * 工具选择模式
     */
    private String toolSelectionMode;

    /**
     * MCP 服务绑定配置列表（含工具级别状态）
     */
    private List<AgentMcpServerBinding> mcpServerBindings;

    // ==================== 偏好设置 ====================

    /**
     * 偏好设置（JSON → Map）
     */
    private Map<String, Object> preferences;

    // ==================== 状态 & 排序 ====================

    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 0=用户自定义, 1=内置
     */
    private Integer builtIn;
    /**
     * 排序序号
     */
    private Integer sortOrder;

    private Long createdAt;
    private Long updatedAt;
}
