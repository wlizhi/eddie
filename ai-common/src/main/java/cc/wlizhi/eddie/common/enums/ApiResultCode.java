/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.enums;

/**
 * API 返回状态码枚举
 */
public enum ApiResultCode implements ResultCode {

    /** 成功 */
    SUCCESS(200, "ok"),

    /** 请求参数错误 */
    BAD_REQUEST(400, "bad request"),

    /** 服务商调用失败 */
    PROVIDER_CALL_FAILED(460, "服务商调用失败，请检查 API 配置"),

    /** 未授权 */
    UNAUTHORIZED(401, "unauthorized"),

    /** 资源不存在 */
    NOT_FOUND(404, "not found"),

    /** 请求冲突（如数据已存在） */
    CONFLICT(409, "conflict"),

    /** 参数校验失败 */
    VALIDATION_FAILED(422, "validation failed"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "internal server error"),

    /** 服务不可用 */
    SERVICE_UNAVAILABLE(503, "service unavailable");

    private final int code;
    private final String message;

    ApiResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
