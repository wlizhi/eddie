package cc.wlizhi.eddie.settings.entity.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 编辑 MCP 服务器请求参数
 * <p>
 * 继承创建参数，增加 id 字段用于定位更新。
 */
@Getter
@Setter
public class McpServerUpdateRequest extends McpServerCreateRequest {

    //    @NotNull(message = "MCP Server ID 不能为空")
    private Long id;
}
