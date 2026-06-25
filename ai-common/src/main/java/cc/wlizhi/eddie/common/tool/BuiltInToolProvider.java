package cc.wlizhi.eddie.common.tool;

/**
 * 内置工具提供者标记接口。<p>
 * 所有包含 {@link org.springframework.ai.tool.annotation.Tool @Tool} 方法的 Bean
 * 都应实现此接口，以便 {@code ToolAutoRegister} 自动发现并注册到数据库。
 */
public interface BuiltInToolProvider {
}
