/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 助手列表表映射实体
 */
@Setter
@Getter
public class AssistantEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 助手名称
     */
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
     * 模型服务商实例 ID（关联 model_provider.id）
     */
    private Long providerId;

    /**
     * 模型 ID，如 "deepseek-v4-flash"
     */
    private String modelId;

    /**
     * 模型参数 JSON：{"temperature":0.7, "maxTokens":2048, ...}
     */
    private String modelParams;

    /**
     * 助手偏好设置 JSON：{"webSearchEnabled":true, "mcpToolMode":"auto", ...}
     * 用于控制前端 UI 默认状态（联网搜索、MCP 模式等）
     */
    private String preferences;

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

    /**
     * 工具选择模式：auto（自动选择）、manual（手动选择）、none（不使用工具）
     */
    private String toolSelectionMode;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
