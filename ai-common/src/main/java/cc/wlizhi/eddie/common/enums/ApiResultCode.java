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

    /** 命令无执行权限（AI 无权执行此指令） */
    COMMAND_NOT_PERMITTED(403, "无权执行此命令"),

    /** 请求超时 */
    TIMEOUT(408, "request timeout"),

    /** 需要用户确认（用于危险操作的双重确认） */
    NEED_CONFIRMATION(490, "需要用户确认"),

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
