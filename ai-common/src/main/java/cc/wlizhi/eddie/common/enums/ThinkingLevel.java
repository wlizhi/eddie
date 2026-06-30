/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * 思考等级
 */
@Getter
public enum ThinkingLevel {

    NONE("none", "无思考"),
    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高");

    private final String code;
    private final String label;

    ThinkingLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
