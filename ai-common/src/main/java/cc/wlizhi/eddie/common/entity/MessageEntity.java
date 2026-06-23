package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录表映射实体
 */
@Getter
@Setter
public class MessageEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 归属会话 ID
     */
    private Long sessionId;

    /**
     * 冗余：归属助手 ID
     */
    private Long assistantId;

    /**
     * user / assistant / system
     */
    private String role;

    /**
     * 模型服务商实例 ID
     */
    private Long providerId;

    /**
     * 模型 code，如 "deepseek-v4-pro"
     */
    private String modelCode;

    /**
     * 模型显示名称
     */
    private String modelName;

    /**
     * 思考内容（DeepSeek reasoning_content）
     */
    private String thinking;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 提示 token 数
     */
    private Integer promptTokens;

    /**
     * 完成 token 数
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;

    /**
     * 预估费用（美元）
     */
    private Double priceEstimate;

    /**
     * 消息时间
     */
    private String createdAt;
}
