/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

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
     * 处理缺少必需的请求参数（如 @RequestParam 注解的必填参数未传递）
     * <p>
     * 返回 400 友好提示，避免客户端看到服务端内部的参数名/类型细节。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必需的请求参数", e);
        return ResponseEntity.badRequest().body(ApiResult.error(ApiResultCode.BAD_REQUEST, "缺少必需的请求参数"));
    }

    /**
     * 处理 @Valid/@Validated 注解校验失败（如 @RequestBody 中字段的 @NotNull 等注解校验未通过）
     * <p>
     * 提取第一个校验失败信息作为提示返回 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("请求参数校验不通过");
        log.warn("请求参数校验失败: {}", message, e);
        return ResponseEntity.badRequest().body(ApiResult.error(ApiResultCode.BAD_REQUEST, message));
    }

    /**
     * 处理 @Validated 注解在方法参数上的校验失败（如 @RequestParam @NotNull 等）
     * <p>
     * 提取第一个校验失败信息作为提示返回 400。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("请求参数校验不通过");
        log.warn("请求参数校验失败: {}", message, e);
        return ResponseEntity.badRequest().body(ApiResult.error(ApiResultCode.BAD_REQUEST, message));
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
     * 处理异步请求响应不可用（客户端断开、Async 超时、容器错误等）
     * <p>
     * 不返回响应体 — 客户端已断开连接，无法接收响应。
     * 仅 WARN 级别日志，避免 ERROR 日志污染。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.warn("异步请求响应不可用（客户端可能已断开）: {}", e.getMessage());
        // void 返回 → Spring 认为异常已处理，不再尝试写响应体
    }

    /**
     * 处理其他未捕获异常（500）
     * <p>
     * 如果响应已提交（如 SSE 流中途出错），不再尝试写入响应体，
     * 避免因 Content-Type 不匹配导致二次异常（{@link HttpMessageNotWritableException}）。
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResult<Void>> handleThrowable(Throwable e, HttpServletResponse response) {
        if (response.isCommitted()) {
            log.warn("响应已提交，无法返回错误响应（客户端可能已断开）: {}", e.getMessage());
            return ResponseEntity.status(499).build();
        }
        log.error("未捕获异常", e);
        return ResponseEntity.internalServerError().body(ApiResult.error(ApiResultCode.INTERNAL_ERROR, e.getMessage()));
    }
}
