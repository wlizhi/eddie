/**
 * @author Eddie
 * {@code @date} 2026-07-15
 */

package cc.wlizhi.eddie.assistant.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 划词助手请求参数
 * <p>
 * 无需传入 providerId / modelId，后端根据 action 自动从全局配置中解析：
 * <ul>
 *   <li>translate → TRANSLATE_MODEL</li>
 *   <li>summarize / explain / beautify → FAST_MODEL → DEFAULT_MODEL（降级）</li>
 * </ul>
 */
@Getter
@Setter
public class SelectionAssistantRequest {

    /**
     * 功能类型：translate / summarize / explain / beautify
     */
    @NotBlank(message = "action 不能为空")
    private String action;

    /**
     * 选中文本
     */
    @NotBlank(message = "text 不能为空")
    private String text;

    /**
     * 翻译专用：源语言（默认 auto 自动检测）
     */
    private String sourceLang = "auto";

    /**
     * 翻译专用：目标语言（默认 zh-CN）
     */
    private String targetLang = "zh-CN";
}
