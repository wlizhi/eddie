package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理系统自定义异常，按异常携带的 ResultCode 和 data 返回
     */
    @ExceptionHandler(AppException.class)
    public ApiResult<Object> handleAppException(AppException e) {
        Object data = e.getData();
        if (data != null) {
            return ApiResult.of(e.getResultCode(), data);
        }
        return ApiResult.error(e.getResultCode(), e.getMessage());
    }

    /**
     * 处理 IllegalArgumentException（视为参数错误 400）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResult.error(ApiResultCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理其他未捕获异常（500）
     */
    @ExceptionHandler(Throwable.class)
    public ApiResult<Void> handleThrowable(Throwable e) {
        log.error("未捕获异常", e);
        return ApiResult.error(ApiResultCode.INTERNAL_ERROR, e.getMessage());
    }
}
