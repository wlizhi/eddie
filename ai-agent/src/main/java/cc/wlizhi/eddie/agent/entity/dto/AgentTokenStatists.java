package cc.wlizhi.eddie.agent.entity.dto;

public class AgentTokenStatists {
    // ==================== Token 统计 ====================

    /**
     * 输入 token 数（prompt）
     */
    private Integer promptTokens;

    /**
     * 输出 token 数（completion）
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;

    /**
     * 缓存读取的 input token 数
     */
    private Integer cacheReadInputTokens;

    /**
     * 缓存写入的 input token 数
     */
    private Integer cacheWriteInputTokens;

    // ==================== 费用估算 ====================

    /**
     * 预估费用
     */
    private Double priceEstimate;

    /**
     * 货币符号（如 ¥ / $）
     */
    private String currency;

    // ==================== 耗时 ====================

    /**
     * 模型调用耗时（毫秒）
     */
    private Integer durationMs;
}
