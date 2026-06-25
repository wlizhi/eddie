package cc.wlizhi.eddie.chat.entity.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 更新助手绑定的 MCP 列表请求参数
 * <p>
 * 全量替换：传入的列表将完全替换该助手已绑定的 MCP 工具。
 */
@Getter
@Setter
public class McpBindingsUpdateRequest {

    /**
     * 启用的 MCP Server ID 列表
     * <p>
     * 传入后会全量替换该助手已绑定的工具源。
     */
    private List<Long> mcpServerIds;
}
