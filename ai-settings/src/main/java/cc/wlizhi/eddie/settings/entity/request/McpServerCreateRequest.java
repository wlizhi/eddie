package cc.wlizhi.eddie.settings.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增 MCP 服务器请求参数
 */
@Getter
@Setter
public class McpServerCreateRequest {

    /**
     * MCP 服务端名称（全局唯一）
     */
    @NotBlank(message = "MCP 服务名称不能为空")
    private String name;

    /**
     * MCP 服务描述
     */
    private String description;

    /**
     * 来源类型：BUILT_IN / USER（默认）/ PROVIDER
     */
    private String sourceType;

    /**
     * 来源配置 JSON（多态），PROVIDER 类型时必填
     */
    private String sourceConfig;

    /**
     * 传输方式：STDIO / SSE / STREAMABLE_HTTP
     */
    @NotBlank(message = "传输方式不能为空")
    private String transportType;

    /**
     * STDIO 启动命令，如 'npx'（STDIO 模式必填）
     */
    private String command;

    /**
     * STDIO 命令参数，JSON 数组
     */
    private String args;

    /**
     * STDIO 环境变量，JSON 对象
     */
    private String env;

    /**
     * SSE / Streamable HTTP 服务端 URL（SSE/HTTP 模式必填）
     */
    private String url;

    /**
     * SSE / Streamable HTTP 自定义请求头，JSON 对象
     */
    private String headers;

    /**
     * 请求超时时间（秒），默认 60
     */
    private Integer timeoutSeconds;

    /**
     * 排序序号，默认 0
     */
    private Integer sortOrder;

    /**
     * 重连间隔(秒)，NULL/0=使用默认5秒
     */
    private Integer reconnectIntervalSec;

    /**
     * 最大重试次数，NULL/0=无限重试
     */
    private Integer maxReconnectAttempts;

    /**
     * 启用状态，默认 false（新建时不连接）
     */
    private Boolean enabled;
}
