package cc.wlizhi.eddie.settings.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * MCP 服务器连接结果（启用/同步时返回）
 * <p>
 * 前端根据此结果展示连接日志：成功时展示工具列表，失败时展示具体报错信息。
 */
@Getter
@Setter
public class McpConnectResult {

    /**
     * 连接是否成功
     */
    private boolean connected;

    /**
     * 提示消息：成功或报错详情
     */
    private String message;

    /**
     * 同步到的工具列表（连接成功时返回）
     */
    private List<McpToolItemVO> tools;

    public static McpConnectResult success(String message, List<McpToolItemVO> tools) {
        McpConnectResult result = new McpConnectResult();
        result.setConnected(true);
        result.setMessage(message);
        result.setTools(tools);
        return result;
    }

    public static McpConnectResult failure(String message) {
        McpConnectResult result = new McpConnectResult();
        result.setConnected(false);
        result.setMessage(message);
        result.setTools(List.of());
        return result;
    }
}
