package cc.wlizhi.eddieai.common.exception;

import cc.wlizhi.eddieai.common.enums.ApiResultCode;

/**
 * 请求冲突异常（409），如数据已存在
 * <p>支持 data 字段携带冲突数据</p>
 */
public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(ApiResultCode.CONFLICT, message);
    }

    public ConflictException(String message, Object data) {
        super(ApiResultCode.CONFLICT, message, data);
    }
}
