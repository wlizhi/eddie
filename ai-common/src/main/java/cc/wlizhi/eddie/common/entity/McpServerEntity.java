package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * MCP 服务器配置表映射实体
 */
@Setter
@Getter
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
     * 请求超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 0=用户自定义(可删除/编辑), 1=内置(不可删除)
     */
    private Integer builtIn;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
