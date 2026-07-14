/**
 * @author Eddie
 * {@code @date} 2026-07-14
 */

package cc.wlizhi.eddie.common.entity.dto;

import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 模型选择：服务商实例 + 模型 ID 配对 + 可选模型参数。
 * <p>
 * 对应 FAST_MODEL / DEFAULT_MODEL 等全局配置项的 JSON 结构：
 * {"providerId": 1, "modelId": "gpt-4o-mini", "modelParams": {...}}
 * <p>
 * {@code modelParams} 结构与 {@link cc.wlizhi.eddie.common.ai.openai.ModelParams} 兼容，
 * 由业务方（如 {@code EddieOpenAiOptionsHelper}）转为 JSON 字符串后按需解析。
 */
@Getter
@Setter
public class ModelSelection {

    /** 模型服务商实例 ID */
    private Long providerId;

    /** 模型 ID（如 "gpt-4o-mini"、"deepseek-chat"） */
    private String modelId;

    /**
     * 模型参数 Map（可选），结构与 {@link cc.wlizhi.eddie.common.ai.openai.ModelParams} 兼容。<p>
     * key 为参数名（如 temperature、maxTokens、topP），value 为对应值。
     * 业务方自行转为 JSON 字符串后传递给 {@code EddieOpenAiOptionsHelper}。
     */
    private ModelParams modelParams;
}
