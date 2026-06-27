package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;

import java.util.List;

/**
 * MCP 工具管理业务接口
 * <p>
 * 对应场景 1-4：全局设置 MCP 列表、启禁、删除、新增。
 */
public interface McpToolService {

    /**
     * 查询全量 MCP + 工具二层列表（含禁用）
     * <p>
     * 数据来源：OwnerToolBindingContext 缓存
     */
    List<McpServerVO> listAll();

    /**
     * 查询 MCP + 工具二层列表（按启用状态筛选）
     * <p>
     * 数据来源：OwnerToolBindingContext 缓存
     *
     * @param enabled null=全量，true=仅已启用，false=仅已禁用
     */
    List<McpServerVO> listAll(Boolean enabled);

    /**
     * 新增 MCP 服务器
     * <p>
     * 自动校验参数完整性、名称唯一性，完成后自动扫描工具并刷新缓存。
     */
    McpServerVO create(McpServerCreateRequest request);

    /**
     * 更新 MCP 或工具的启用状态（级联联动）
     * <p>
     * 自动执行级联规则：工具全部禁用 → MCP 禁用；任意工具启用 → MCP 启用。
     */
    void updateStatus(McpStatusUpdateRequest request);

    /**
     * 删除 MCP 服务器（级联删除工具和绑定关系）
     * <p>
     * builtIn=1 的内置 MCP 不可删除。
     */
    void delete(Long id);
}
