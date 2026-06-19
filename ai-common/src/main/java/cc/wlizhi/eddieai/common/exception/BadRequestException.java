package cc.wlizhi.eddieai.common.exception;

import cc.wlizhi.eddieai.common.enums.ApiResultCode;

/**
 * 请求参数错误异常（400）
 */
public class BadRequestException extends AppException {

    public BadRequestException() {
        super(ApiResultCode.BAD_REQUEST);
    }

    public BadRequestException(String message) {
        super(ApiResultCode.BAD_REQUEST, message);
    }
}
