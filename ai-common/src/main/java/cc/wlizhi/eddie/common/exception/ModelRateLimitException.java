package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ResultCode;

public class ModelRateLimitException extends AppException{
    public ModelRateLimitException(ResultCode resultCode) {
        super(resultCode);
    }

    public ModelRateLimitException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public ModelRateLimitException(ResultCode resultCode, String message, Throwable originException) {
        super(resultCode, message, originException);
    }
}
