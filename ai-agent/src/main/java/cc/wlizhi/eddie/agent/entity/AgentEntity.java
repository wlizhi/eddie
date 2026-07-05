/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体元数据 — 映射 ai_agent 表
 */
@Setter
@Getter
public class AgentEntity {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private String systemPrompt;

    /**
     * 主模型服务商实例 ID
     */
    private Long mainProviderId;
    /**
     * 主模型 ID
     */
    private String mainModelId;
    /**
     * 主模型参数 JSON
     */
    private String mainModelParams;

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
     * FOREGROUND / BACKGROUND
     */
    private String executionMode;

    /**
     * 工具选择模式
     */
    private String toolSelectionMode;

    /**
     * 偏好设置 JSON
     */
    private String preferences;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;
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
