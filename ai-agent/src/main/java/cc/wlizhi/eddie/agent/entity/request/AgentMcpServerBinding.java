/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.agent.entity.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 智能体 MCP 服务绑定配置
 * <p>
 * 指定一个 MCP 服务及其下辖工具的绑定状态。
 */
@Getter
@Setter
public class AgentMcpServerBinding {

    /** MCP 服务 ID */
    private Long mcpServerId;

    /** 该 MCP 服务下的工具绑定列表 */
    private List<AgentToolBinding> tools;
}
