/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.dto;

import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.enums.ResultCode;
import lombok.Getter;

/**
 * 通用 API 响应封装。
 * <p>
 * REST 接口与 SSE 事件推送共用此结构，<code>detail</code> 字段携带技术排查信息，
 * 仅在本机调试或日志记录时使用，前端不应直接展示给用户。
 */
@Getter
public class ApiResult<T> {

    private int code;
    private String message;
    private String detail;
    private T data;

    public ApiResult() {
    }

    public static <T> ApiResult<T> success(T data) {
        return of(ApiResultCode.SUCCESS, data);
    }

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> error(ResultCode resultCode) {
        return of(resultCode, null);
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = message;
        return result;
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, String message, String detail) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = message;
        result.detail = detail;
        return result;
    }

    public static <T> ApiResult<T> of(ResultCode resultCode, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = resultCode.getMessage();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> of(ResultCode resultCode, String detail, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = resultCode.getMessage();
        result.detail = detail;
        result.data = data;
        return result;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setData(T data) {
        this.data = data;
    }
}
