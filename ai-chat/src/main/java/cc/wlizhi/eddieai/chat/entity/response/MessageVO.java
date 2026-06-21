package cc.wlizhi.eddieai.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录响应 VO
 */
@Getter
@Setter
public class MessageVO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * user / assistant / system
     */
    private String role;

    /**
     * 模型服务商实例 ID
     */
    private Long providerId;

    /**
     * 模型 code
     */
    private String modelCode;

    /**
     * 模型显示名称
     */
    private String modelName;

    /**
     * 思考内容
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
