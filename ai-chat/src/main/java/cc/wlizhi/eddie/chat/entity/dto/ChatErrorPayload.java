/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.chat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * error 事件 Payload — 错误信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatErrorPayload {

    /** 错误提示消息 */
    private String message;
}
