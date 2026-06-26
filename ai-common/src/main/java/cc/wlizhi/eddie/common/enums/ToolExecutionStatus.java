/**
 * ToolExecutionStatus — 工具执行事件状态枚举
 * <p>
 * 替代硬编码的 "start" / "complete" 字符串。
 * 序列化时使用 name() 小写形式，兼容前端已有逻辑。
 */
package cc.wlizhi.eddie.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ToolExecutionStatus {

    START("start"),
    COMPLETE("complete");

    private final String value;

    ToolExecutionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ToolExecutionStatus fromValue(String value) {
        for (ToolExecutionStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ToolExecutionStatus: " + value);
    }
}
