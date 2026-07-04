/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 内置工具配置字段描述。<p>
 * 用于 {@link ConfigSchema} 中描述单个可配参数的信息，
 * 前端可根据此描述动态渲染配置表单。
 *
 * @author Eddie
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFieldDescriptor {

    /**
     * 字段名（对应 source_config JSON 的 key）
     */
    private String name;

    /**
     * 字段类型：string / number / boolean / select / textarea
     */
    private String type;

    /**
     * 前端显示标签
     */
    private String label;

    /**
     * 前端提示说明文案
     */
    private String description;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 输入框占位文本（仅 string / textarea 类型）
     */
    private String placeholder;

    /**
     * 是否必填（默认 false）
     */
    private Boolean required;

    /**
     * 最小值（仅 number 类型）
     */
    private Integer min;

    /**
     * 最大值（仅 number 类型）
     */
    private Integer max;

    /**
     * select 类型的选项列表（仅 select 类型）
     */
    private List<SelectOption> options;

    /**
     * 依赖的字段名。<p>
     * 非空时，前端仅当依赖字段的值等于 {@link #dependsOnValue} 时才显示该字段。
     * 用于实现字段间的条件可见性。
     */
    private String dependsOn;

    /**
     * 依赖字段的目标值。与 {@link #dependsOn} 配合使用。
     */
    private Object dependsOnValue;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectOption {
        /** 选项值 */
        private String value;
        /** 显示标签 */
        private String label;
    }
}
