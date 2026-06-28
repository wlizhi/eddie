package cc.wlizhi.eddie.chat.entity.response;

import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

    /**
     * 工具调用记录列表（由 JSON 字符串反序列化而来）
     */
    private List<ToolExecutionEvent> toolCalls;

    /**
     * 缓存读取的 input token 数
     */
    private Integer cacheReadInputTokens;

    /**
     * 缓存写入的 input token 数
     */
    private Integer cacheWriteInputTokens;

    /**
     * 费用货币符号，如 ¥ / $
     */
    private String currency;

    /**
     * 接口耗时（毫秒）
     */
    private Integer durationMs;
}
