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
 * cancelled 事件 Payload — 回答被取消
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelledPayload {

    /** 取消原因 */
    private String reason;
}
