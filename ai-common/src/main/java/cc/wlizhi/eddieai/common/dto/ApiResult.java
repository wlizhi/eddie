package cc.wlizhi.eddieai.common.dto;

import cc.wlizhi.eddieai.common.enums.ApiResultCode;
import cc.wlizhi.eddieai.common.enums.ResultCode;
import lombok.Getter;

/**
 * 通用 API 响应封装
 */
@Getter
public class ApiResult<T> {

    private int code;
    private String message;
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

    public static <T> ApiResult<T> of(ResultCode resultCode, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = resultCode.getMessage();
        result.data = data;
        return result;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }
}
