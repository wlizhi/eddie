/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.common.tool;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 工具行为声明。<p>
 * 描述一个工具的可识别操作及其默认安全属性。
 * 工具通过 {@link BuiltInToolProvider#getBehaviors()} 返回行为列表，
 * 用户可在工具设置页中覆盖每个行为的默认安全级别。
 * <p>
 * 拦截器在运行时解析工具调用参数（toolInput JSON），
 * 通过 {@link #discriminatorField} 和 {@link #discriminatorValue} 匹配当前调用属于哪个行为，
 * 然后按三层决策逻辑决定是否需审批：
 * <ol>
 *   <li>助手级 {@code enabled=2}（整个工具需审批）→ 所有行为需审批</li>
 *   <li>用户配置覆盖 → 按用户配置</li>
 *   <li>代码默认值 → 按 {@link #defaultSecurity}</li>
 * </ol>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToolBehavior {

    /**
     * 行为标识，如 "read"
     */
    private String name;

    /**
     * 行为描述，如 "读取系统剪贴板"
     */
    private String description;

    /**
     * 从 toolInput JSON 中识别此行为的字段名。
     * 例如 "action"：表示从 {@code {"action":"read"}} 中取 action 字段值来匹配。
     */
    private String discriminatorField;

    /**
     * 匹配此行为时 {@link #discriminatorField} 的值。
     * 例如 "read"：表示 {@code {"action":"read"}} 匹配 "read" 行为。
     */
    private String discriminatorValue;

    /**
     * 默认安全级别（用户未配置时生效）
     */
    private SecurityLevel defaultSecurity;

    /**
     * 安全级别
     */
    public enum SecurityLevel {
        /**
         * 自动放行，无需审批
         */
        AUTO,
        /**
         * 需用户审批
         */
        APPROVAL,
        /**
         * 直接拒绝
         */
        DENY
    }
}
