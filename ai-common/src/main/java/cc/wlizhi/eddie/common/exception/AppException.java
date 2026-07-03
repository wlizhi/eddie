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

    /**
     * 原始异常（如 HTTP 客户端异常等），用于保留根因
     */
    private final Throwable originException;

    public AppException() {
        this.resultCode = ApiResultCode.INTERNAL_ERROR;
        this.data = null;
        this.originException = null;
    }

    public AppException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = null;
        this.originException = null;
    }

    public AppException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.data = null;
        this.originException = null;
    }

    public AppException(ResultCode resultCode, String message, Throwable originException) {
        super(message, originException);
        this.resultCode = resultCode;
        this.data = null;
        this.originException = originException;
    }

    public AppException(ResultCode resultCode, String message, Object data, Throwable originException) {
        super(message, originException);
        this.resultCode = resultCode;
        this.data = data;
        this.originException = originException;
    }

    public AppException(ResultCode resultCode, String message, Object data) {
        super(message);
        this.resultCode = resultCode;
        this.data = data;
        this.originException = null;
    }

    public AppException(ResultCode resultCode, Object data) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = data;
        this.originException = null;
    }

    public AppException(String message) {
        super(message);
        this.resultCode = ApiResultCode.INTERNAL_ERROR;
        this.data = null;
        this.originException = null;
    }

}
