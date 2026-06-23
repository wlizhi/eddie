package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ApiResultCode;

/**
 * 资源不存在异常（404）
 */
public class NotFoundException extends AppException {

    public NotFoundException() {
        super(ApiResultCode.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ApiResultCode.NOT_FOUND, message);
    }
}
