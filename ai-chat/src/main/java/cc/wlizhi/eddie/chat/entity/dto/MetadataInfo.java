/**
 * MetadataInfo — 响应元数据值对象
 * <p>
 * 由 ChatMetadataHandler 在流式响应结束后构建，
 * 作为单一数据源同时供 SSE 事件推送和消息持久化使用，
 * 保证两路数据一致。
 */
package cc.wlizhi.eddie.chat.entity.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataInfo {

    /**
     * 本轮对话耗时（毫秒）
     */
    private long durationMs;

    /**
     * 结束时间戳（毫秒）
     */
    private long timestamp;

    /**
     * prompt token 数
     */
    private int promptTokens;

    /**
     * completion token 数
     */
    private int completionTokens;

    /**
     * 总 token 数
     */
    private int totalTokens;

    /**
     * 缓存读取的 input token 数
     */
    private int cacheReadInputTokens;

    /**
     * 缓存写入的 input token 数
     */
    private int cacheWriteInputTokens;

    /**
     * 预估费用
     */
    private double costEstimate;

    /**
     * 费用货币符号，如 ¥ / $
     */
    private String currency;
}
