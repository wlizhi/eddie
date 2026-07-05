/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.common.ai.openai;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型参数 DTO（通用参数 + 扩展参数）
 * <p>
 * Tier 1 通用参数：几乎所有服务商支持
 * Tier 2 常见参数：部分服务商支持
 * extensions：服务商特有参数兜底
 * <p>
 * 所有字段为 null 时不设置，由 ChatPolicy 或 API 服务端决定默认值。
 */
@Getter
@Setter
public class ModelParams {

    // ==================== Tier 1: 几乎所有服务商支持 ====================

    /**
     * 0~2，控制输出随机性
     */
    private Double temperature;

    /**
     * 最大输出 token 数
     */
    private Integer maxTokens;

    /**
     * 0~1，核采样（nucleus sampling）
     */
    private Double topP;

    // ==================== Tier 2: 部分服务商支持 ====================

    /**
     * -2~2，频率惩罚（OpenAI / DeepSeek）
     */
    private Double frequencyPenalty;

    /**
     * -2~2，存在惩罚（OpenAI / DeepSeek）
     */
    private Double presencePenalty;

    /**
     * Top-K（Anthropic / Ollama / Mistral）
     */
    private Integer topK;

    /**
     * 停止序列
     */
    private List<String> stop;

    /**
     * 思考模式：auto / low / medium / high / max / disabled
     * <p>
     * - auto: 不传递参数，让模型自己决定
     * - low/medium/high: 对应 reasoning_effort 参数
     * - max: 最大思考力度（DeepSeek 特有）
     * - disabled: 禁用思考
     * <p>
     * 优先级高于助手的 modelParams 中的配置。
     */
    private String thinkingMode;

    // ==================== 扩展参数 ====================

    /**
     * 服务商特有参数，如 ollama 的 mirostat
     */
    private Map<String, Object> extensions = new HashMap<>();
}
