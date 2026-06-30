/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.enums.ResultCode;
import lombok.Getter;

/**
 * 系统全局异常基类
 * <p>继承 RuntimeException，支持 data 字段携带业务侧自定义数据</p>
 */
@Getter
public class AppException extends RuntimeException {

    private final ResultCode resultCode;
    /**
     * -- GETTER --
     *  获取业务侧自定义数据
     */
    private final Object data;

    public AppException() {
        this.resultCode = ApiResultCode.INTERNAL_ERROR;
        this.data = null;
    }

    public AppException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = null;
    }

    public AppException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.data = null;
    }

    public AppException(ResultCode resultCode, String message, Object data) {
        super(message);
        this.resultCode = resultCode;
        this.data = data;
    }

    public AppException(ResultCode resultCode, Object data) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = data;
    }

    public AppException(String message) {
        super(message);
        this.resultCode = ApiResultCode.INTERNAL_ERROR;
        this.data = null;
    }

}
