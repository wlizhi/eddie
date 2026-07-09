/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.common.enums;

/**
 * 工具启用状态枚举（替代 enabled 字段的 0/1 二元值）
 * <p>
 * 扩展为三态：
 * <ul>
 *   <li>{@link #DISABLED} — 禁用，工具不可用、模型不可见</li>
 *   <li>{@link #ENABLED} — 启用，工具可直接调用</li>
 *   <li>{@link #PENDING_APPROVAL} — 待审批，模型可见可调用，但需用户批准后才执行</li>
 * </ul>
 */
public enum ToolEnabledStatus {

    DISABLED(0),
    ENABLED(1),
    PENDING_APPROVAL(2);

    private final int code;

    ToolEnabledStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ToolEnabledStatus fromCode(int code) {
        for (ToolEnabledStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ToolEnabledStatus code: " + code);
    }

    /**
     * 判断该状态是否算作"可用"（模型可见、可调用）
     */
    public boolean isAvailable() {
        return this == ENABLED || this == PENDING_APPROVAL;
    }
}
