/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 内置工具配置描述 Schema。<p>
 * 描述一个内置工具有哪些可配置参数、分别是什么类型。
 * 前端根据此 Schema 动态渲染配置表单。
 * 用户配置的值存储在对应的 MCP Server 的 {@code source_config} 字段中。
 *
 * @author Eddie
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSchema {

    /** 配置面板标题 */
    private String title;

    /** 配置面板说明 */
    private String description;

    /** 字段列表，非空表示该工具支持配置 */
    private List<ConfigFieldDescriptor> fields;

    /** 返回一个空的 schema（表示该工具不支持配置） */
    public static ConfigSchema empty() {
        return new ConfigSchema("", "", Collections.emptyList());
    }

    /** 该工具是否支持配置 */
    public boolean isConfigurable() {
        return fields != null && !fields.isEmpty();
    }
}
