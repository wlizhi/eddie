/**
 * @author Eddie
 * {@code @date} 2026-07-03
 */

package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ResultCode;

/**
 * 服务商调用异常
 * <p>
 * 远程 API 调用（如拉取模型列表）失败时抛出。
 * 额外数据（httpStatus、responseBody、providerCode 等）通过 {@link #getData()} 承载。
 * 原始异常（通常是 {@link org.springframework.web.client.HttpClientErrorException}）
 * 通过 {@link #getOriginException()} 获取。
 */
public class ProviderCallException extends AppException {

    public ProviderCallException(ResultCode resultCode, String message, Object data, Throwable origin) {
        super(resultCode, message, data, origin);
    }
}
