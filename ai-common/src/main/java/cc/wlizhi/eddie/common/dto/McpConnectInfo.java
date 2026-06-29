package cc.wlizhi.eddie.common.dto;

import lombok.Getter;

import java.util.List;

/**
 * MCP 服务器同步连接结果（跨模块传输，不含 VO 层转化逻辑）
 * <p>
 * 由 {@code McpClientRegistry} 返回，业务层再转为 {@code McpConnectResult} VO。
 */
@Getter
public class McpConnectInfo {

    private final boolean connected;
    private final String message;
    private final List<ToolInfo> tools;

    private McpConnectInfo(boolean connected, String message, List<ToolInfo> tools) {
        this.connected = connected;
        this.message = message;
        this.tools = tools;
    }

    public static McpConnectInfo success(List<ToolInfo> tools) {
        return new McpConnectInfo(true, "连接成功", tools);
    }

    public static McpConnectInfo failure(String message) {
        return new McpConnectInfo(false, message, List.of());
    }

    /**
     * MCP 协议返回的单个工具基本信息
     */
    @Getter
    public static class ToolInfo {

        /**
         * 工具唯一标识名
         */
        private final String name;

        /**
         * 工具描述
         */
        private final String description;

        /**
         * JSON Schema 格式的输入参数定义
         */
        private final String inputSchema;

        public ToolInfo(String name, String description, String inputSchema) {
            this.name = name;
            this.description = description != null ? description : "";
            this.inputSchema = inputSchema != null ? inputSchema : "{}";
        }
    }
}
