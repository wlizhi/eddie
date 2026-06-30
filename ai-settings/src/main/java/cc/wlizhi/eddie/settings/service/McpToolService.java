/**
 * @author Eddie
 * {@code @date} 2026-06-26
 */

package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.settings.entity.request.BuiltInStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpConnectResult;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
import cc.wlizhi.eddie.settings.entity.response.McpToolItemVO;

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
     * 当启用 MCP 时，会自动进行 MCP 协议连接并同步远端工具列表到 DB。
     *
     * @return 连接结果（启用时返回连接日志，禁用时 connected=true）
     */
    McpConnectResult updateStatus(McpStatusUpdateRequest request);

    /**
     * 删除 MCP 服务器（级联删除工具和绑定关系）
     * <p>
     * builtIn=1 的内置 MCP 不可删除。
     */
    void delete(Long id);

    /**
     * 查询指定 MCP 服务下的工具列表（从 DB 缓存）
     *
     * @param mcpServerId MCP 服务器 ID
     * @return 工具列表
     */
    List<McpToolItemVO> listToolsByMcpServer(Long mcpServerId);

    /**
     * 手动同步 MCP 服务器工具：重新连接 MCP 协议并同步远端工具到 DB
     * <p>
     * 覆盖已注册的工具定义（按 name 去重），删除不再提供的工具。
     *
     * @param mcpServerId MCP 服务器 ID
     * @return 同步后的连接结果（含工具列表）
     */
    McpConnectResult syncTools(Long mcpServerId);

    /**
     * 测试 MCP 服务器连接：仅连接验证，不写入数据库，不改变启用/禁用状态。
     * <p>
     * 连接成功时返回远端工具列表用于前端展示；连接失败时返回具体错误信息。
     *
     * @param request MCP 服务器连接参数（无需 ID）
     * @return 连接结果（含工具列表）
     */
    McpConnectResult testConnection(McpServerCreateRequest request);

    /**
     * 编辑 MCP 服务器（全量覆盖更新）
     * <p>
     * 保存时根据 enabled 状态决定是否连接：
     * enabled=true → 尝试连接并同步工具到 DB；
     * enabled=false → 仅更新表单字段，断开连接。
     *
     * @param request 编辑请求参数（含 id）
     * @return 更新后的 MCP 服务 VO
     */
    McpServerVO update(McpServerUpdateRequest request);

    /**
     * 内置工具启用/禁用切换
     * <p>
     * 内置工具不涉及 MCP 连接，仅更新工具本身的 enabled 状态并刷新缓存。
     *
     * @param request 内置工具状态更新请求
     */
    void updateBuiltInStatus(BuiltInStatusUpdateRequest request);
}
