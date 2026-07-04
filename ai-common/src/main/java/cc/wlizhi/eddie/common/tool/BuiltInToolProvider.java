/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.common.tool;

import cc.wlizhi.eddie.common.dto.ConfigSchema;

/**
 * 内置工具提供者标记接口。<p>
 * 所有包含 {@link org.springframework.ai.tool.annotation.Tool @Tool} 方法的 Bean
 * 都应实现此接口，以便 {@code ToolAutoRegister} 自动发现并注册到数据库。
 */
public interface BuiltInToolProvider {

    /**
     * 返回此工具提供者归属的逻辑 MCP 名称。
     * <p>
     * 同一 MCP 名称下的多个工具提供者会被归到同一个 MCP Server 分组。
     * 例如 {@code WebSearchTools} 和 {@code WebFetchTools} 都返回 "BuiltInSearch"，
     * 则它们的工具都属于同一个 "BuiltInSearch" 源。
     */
    default String getMcpServerName() {
        return getClass().getSimpleName()
                .replaceAll("Tools?$", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
    }

    /**
     * 返回此工具的配置描述 Schema。<p>
     * 返回非空 Schema（{@link ConfigSchema#isConfigurable()} 为 {@code true}）表示该工具支持用户配置。
     * 前端将根据 Schema 动态渲染配置表单，用户保存的值存储在 MCP Server 的 {@code source_config} 字段中。
     * <p>
     * 默认返回空 Schema（不支持配置）。
     */
    default ConfigSchema getConfigSchema() {
        return ConfigSchema.empty();
    }
}
