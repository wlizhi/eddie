package cc.wlizhi.eddie.settings.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * MCP 服务器 + 下辖工具的二层结构 VO
 * <p>
 * 用于全局设置页面展示 MCP 及工具列表。
 */
@Getter
@Setter
public class McpServerVO {

    /**
     * MCP Server ID
     */
    private Long id;

    /**
     * MCP 服务端名称
     */
    private String name;

    /**
     * 传输方式：STDIO / SSE / STREAMABLE_HTTP
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
     * 当前全局启用状态
     */
    private Boolean enabled;

    /**
     * 0=用户自定义(可删除/编辑), 1=内置(不可删除)
     */
    private Boolean builtIn;

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

    /**
     * 该 MCP 下的工具列表
     */
    private List<McpToolItemVO> tools;
}
