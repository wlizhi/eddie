/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MCP 服务器配置表映射实体
 */
@Setter
@Getter
@NoArgsConstructor
public class McpServerEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * MCP 服务端名称
     */
    private String name;

    /**
     * MCP 服务描述
     */
    private String description;

    /**
     * 传输方式：STDIO / SSE / STREAMABLE_HTTP / BUILT_IN
     */
    private String transportType;

    /**
     * STDIO 启动命令，如 'npx'
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
     * SSE / Streamable HTTP 服务端 URL
     */
    private String url;

    /**
     * SSE / Streamable HTTP 自定义请求头，JSON 对象
     */
    private String headers;

    /**
     * 请求超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 来源类型：BUILT_IN（内置工具）/ USER（用户自定义）/ PROVIDER（第三方服务商）
     */
    private String sourceType;

    /**
     * 来源配置 JSON（多态）：
     * BUILT_IN → "{}"（无配置）
     * USER     → "{}"（配置在 command/args/env/url/headers 等标准字段）
     * PROVIDER → {"auth_type":"...", "credentials":{...}, "provider_code":"...", ...}
     */
    private String sourceConfig;

    /**
     * 排序序号
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
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
