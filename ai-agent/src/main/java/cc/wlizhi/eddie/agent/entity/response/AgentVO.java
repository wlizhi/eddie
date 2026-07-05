/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 智能体列表展示 VO
 */
@Getter
@Setter
public class AgentVO {

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
     * 主模型服务商名称
     */
    private String mainProviderName;
    /**
     * 主模型 ID
     */
    private String mainModelId;

    /**
     * 执行模式
     */
    private String executionMode;
    /**
     * 工具选择模式
     */
    private String toolSelectionMode;

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
