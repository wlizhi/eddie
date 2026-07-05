/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.common.enums.agent;

import lombok.Getter;

/**
 * 工具选择模式枚举
 * <p>
 * AUTO — 自动选择工具
 * MANUAL — 手动选择工具
 * NONE — 不使用工具
 */
@Getter
public enum ToolSelectionMode {

    AUTO("自动选择"),
    MANUAL("手动选择"),
    NONE("不使用工具");

    private final String label;

    ToolSelectionMode(String label) {
        this.label = label;
    }

    /**
     * 根据 name 反查枚举（忽略大小写）
     */
    public static ToolSelectionMode fromCode(String code) {
        if (code == null) return null;
        for (ToolSelectionMode m : values()) {
            if (m.name().equalsIgnoreCase(code)) {
                return m;
            }
        }
        return null;
    }
}
