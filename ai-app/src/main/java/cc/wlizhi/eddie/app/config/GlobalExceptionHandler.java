package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

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
    public ResponseEntity<ApiResult<Object>> handleAppException(AppException e) {
        Object data = e.getData();
        int httpStatus = e.getResultCode().getCode();
        if (data != null) {
            return ResponseEntity.status(httpStatus).body(ApiResult.of(e.getResultCode(), data));
        }
        return ResponseEntity.status(httpStatus).body(ApiResult.error(e.getResultCode(), e.getMessage()));
    }

    /**
     * 处理 IllegalArgumentException（视为参数错误 400）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResult.error(ApiResultCode.BAD_REQUEST, e.getMessage()));
    }

    /**
     * 处理 HTTP 客户端异常（4xx），如认证失败、请求被拒等
     * <p>
     * 提取远程 API 返回的响应体中的错误信息，返回友好的提示。
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpClientError(HttpClientErrorException e) {
        // 提取远程 API 返回的错误详情
        String responseBody = e.getResponseBodyAsString();
        String detail = (responseBody != null && !responseBody.isBlank())
                ? responseBody.trim()
                : e.getStatusText();
        HttpStatus status = (HttpStatus) e.getStatusCode();

        if (status == HttpStatus.UNAUTHORIZED) {
            log.warn("远程 API 认证失败: {}", detail);
            String message = "API 认证失败，请检查 API Key 是否正确";
            if (detail.length() < 200) {
                message += "（" + detail + "）";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error(ApiResultCode.UNAUTHORIZED, message));
        }

        log.warn("远程 API 请求失败 [{}]: {}", status.value(), detail);
        return ResponseEntity.status(status)
                .body(ApiResult.error(ApiResultCode.BAD_REQUEST, "远程 API 请求失败: " + detail));
    }

    /**
     * 处理其他未捕获异常（500）
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResult<Void>> handleThrowable(Throwable e) {
        log.error("未捕获异常", e);
        return ResponseEntity.internalServerError().body(ApiResult.error(ApiResultCode.INTERNAL_ERROR, e.getMessage()));
    }
}
