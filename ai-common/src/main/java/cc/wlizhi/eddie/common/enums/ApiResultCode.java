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
    SERVICE_UNAVAILABLE(503, "service unavailable"),

    // ==================== SSE Agent 事件异常码 (1000-1999) ====================

    /** SSE 事件序列化失败（ObjectMapper 写 JSON 异常） */
    AGENT_EVENT_SERIALIZATION_ERROR(1000, "SSE 事件序列化失败"),

    /** 流处理异常（thinking/answer 处理过程中出错） */
    AGENT_STREAM_PROCESSING_ERROR(1100, "流处理异常"),

    /** 工具执行失败 */
    AGENT_TOOL_EXECUTION_ERROR(1200, "工具执行失败"),

    /** 任务计划生成失败 */
    AGENT_PLAN_GENERATION_ERROR(1300, "任务计划生成失败"),

    /** 任务被中断（用户取消/线程中断） */
    AGENT_TASK_ABORTED(1400, "任务被中断"),

    /** 智能体内部未知错误 */
    AGENT_INTERNAL_ERROR(1500, "智能体内部错误");

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
