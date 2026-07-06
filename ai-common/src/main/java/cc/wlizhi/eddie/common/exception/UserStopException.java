/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ApiResultCode;

/**
 * 用户点击"终止回答"按钮时抛出的业务异常
 * <p>
 * 继承 {@link AppException}，在全局异常处理器中仅打印一行 info 日志，
 * 不会触发 warn/error 告警，也不会向前端返回错误响应。
 * <p>
 * 用于替代之前的 RuntimeException 中断方式，使中断语义清晰明确。
 */
public class UserStopException extends AppException {

    public UserStopException() {
        super(ApiResultCode.SUCCESS, "用户已停止回答");
    }

    public UserStopException(String message) {
        super(ApiResultCode.SUCCESS, message);
    }
}
