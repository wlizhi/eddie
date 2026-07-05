/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 模型调用信息 — 记录单次模型调用的 Token 统计数据
 * <p>
 * 对应 ai_agent_session_msg 表中的 Token 统计字段，
 * 包含 prompt/completion token 数、缓存命中数、费用估算和耗时。
 */
@Getter
@Setter
public class AgentModelInfo {

    /**
     * 模型 ID
     */
    private String id;

    /**
     * 模型展示名称
     */
    private String name;

    /**
     * 对象类型，通常为 "model"
     */
    private String object;

    /**
     * 模型归属方
     */
    private String ownedBy;

    /**
     * 模型能力标签列表，如 ["function_calling", "vision"]
     */
    private List<String> capabilities;

    /**
     * 币种符号，如 ¥ / $
     */
    private String currency;

    /**
     * 输入价格（每百万 token）
     */
    private Double inputPrice;

    /**
     * 输出价格（每百万 token）
     */
    private Double outputPrice;

    /**
     * 缓存命中价格（每百万 token）
     */
    private Double cacheInputPrice;

    /**
     * 缓存写入价格（每百万 token）
     */
    private Double cacheWriteInputPrice;

    /**
     * 调用最小间隔（秒），null 表示不限制
     */
    private Integer callIntervalSec;
}
