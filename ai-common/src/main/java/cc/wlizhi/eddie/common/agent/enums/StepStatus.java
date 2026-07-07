package cc.wlizhi.eddie.common.agent.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 步骤执行状态枚举 — 对应 AgentTaskStep.status 字段及 AgentStepStreamContext.stepStatus 缓冲字段。
 * <p>
 * JSON 序列化时输出小写字符串，与模型输出格式兼容。
 */
@Getter
public enum StepStatus {

    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    ;

    private final String value;

    StepStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StepStatus fromValue(String value) {
        for (StepStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知 StepStatus: " + value);
    }
}
