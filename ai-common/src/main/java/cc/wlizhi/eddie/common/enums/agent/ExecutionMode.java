/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.common.enums.agent;

import lombok.Getter;

/**
 * 智能体执行模式枚举
 * <p>
 * FOREGROUND — 纯前台执行。用户切换页面导致 SSE 中断则任务中止
 * BACKGROUND — 前台+后台。SSE 断开后任务继续执行，用户可随时回来看进度
 */
@Getter
public enum ExecutionMode {

    FOREGROUND("纯前台"),
    BACKGROUND("前台+后台");

    private final String label;

    ExecutionMode(String label) {
        this.label = label;
    }

    /**
     * 根据 name 反查枚举（忽略大小写）
     */
    public static ExecutionMode fromCode(String code) {
        if (code == null) return null;
        for (ExecutionMode m : values()) {
            if (m.name().equalsIgnoreCase(code)) {
                return m;
            }
        }
        return null;
    }
}
