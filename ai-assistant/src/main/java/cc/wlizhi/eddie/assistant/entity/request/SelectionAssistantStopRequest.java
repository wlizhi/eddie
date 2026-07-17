/**
 * @author Eddie
 * {@code @date} 2026-07-17
 */

package cc.wlizhi.eddie.assistant.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * 划词助手停止请求参数
 * <p>
 * 前端切换目标语言 / 重新生成时，先调用 stop 接口优雅终止当前流。
 */
@Getter
@Setter
public class SelectionAssistantStopRequest {

    /**
     * 功能类型：translate / summarize / explain / beautify
     */
    @NotBlank(message = "action 不能为空")
    private String action;

    /**
     * 会话唯一序列号（由 stream 接口的 start 事件发射给前端）
     */
    @Positive(message = "sequenceId 必须为正数")
    private int sequenceId;
}
