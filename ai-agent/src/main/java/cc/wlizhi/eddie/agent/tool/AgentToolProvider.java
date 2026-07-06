/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.tool;

/**
 * 智能体内置工具提供者标记接口。<p>
 * 与 {@link cc.wlizhi.eddie.common.tool.BuiltInToolProvider} 不同，实现此接口的工具
 * 是智能体运行必需的，不可由用户禁用/启用，始终注入到智能体的 ChatClient 中。
 * <p>
 * 实现类需包含 {@link org.springframework.ai.tool.annotation.Tool @Tool} 方法，
 * 通过 {@link org.springframework.ai.support.ToolCallbacks#from(Object)} 自动发现。
 */
public interface AgentToolProvider {
}
