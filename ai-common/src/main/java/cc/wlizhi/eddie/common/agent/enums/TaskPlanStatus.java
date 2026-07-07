package cc.wlizhi.eddie.common.agent.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务计划状态枚举 — 对应 AgentTaskPlan.status 字段。
 * <p>
 * JSON 序列化时输出小写字符串，与模型输出格式兼容。
 */
@Getter
public enum TaskPlanStatus {

    PLANNED("planned"),
    EXECUTING("executing"),
    COMPLETED("completed"),
    FAILED("failed"),
    ;

    private final String value;

    TaskPlanStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
