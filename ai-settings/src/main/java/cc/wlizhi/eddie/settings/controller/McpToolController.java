package cc.wlizhi.eddie.settings.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.settings.entity.request.BuiltInStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpServerUpdateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpConnectResult;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
import cc.wlizhi.eddie.settings.entity.response.McpToolItemVO;
import cc.wlizhi.eddie.settings.service.McpToolService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP 工具管理 API
 * <p>
 * 对应场景 1-4：全局设置 MCP 列表、启禁、删除、新增。
 */
@RestController
@RequestMapping("/api/mcp-servers")
public class McpToolController {

    @Resource
    private McpToolService mcpToolService;

    /**
     * 查询 MCP + 工具二层列表
     * <p>
     * 场景 1：全局设置页面展示 MCP 及工具。
     *
     * @param enabled 可选，null=全量，true=仅已启用，false=仅已禁用
     */
    @GetMapping
    public ApiResult<List<McpServerVO>> listAll(
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return ApiResult.success(mcpToolService.listAll(enabled));
    }

    /**
     * 新增 MCP 服务器
     * <p>
     * 场景 4：用户自定义添加 MCP 工具。
     * STDIO 模式需传 command，SSE/HTTP 模式需传 url。
     */
    @PostMapping
    public ApiResult<McpServerVO> create(@Valid @RequestBody McpServerCreateRequest request) {
        return ApiResult.success(mcpToolService.create(request));
    }

    /**
     * 更新 MCP 或工具的启用状态（级联联动）
     * <p>
     * 场景 2：启用或禁用 MCP 或工具。
     * 工具全部禁用 → MCP 自动禁用；任意工具启用 → MCP 自动启用。
     * 启用 MCP 时自动进行协议连接，并返回连接结果（含工具同步状态）。
     */
    @PatchMapping("/status")
    public ApiResult<McpConnectResult> updateStatus(@Valid @RequestBody McpStatusUpdateRequest request) {
        McpConnectResult result = mcpToolService.updateStatus(request);
        return ApiResult.success(result);
    }

    /**
     * 删除 MCP 服务器（级联删除工具和绑定关系）
     * <p>
     * 场景 3：删除 MCP 及其下所有工具。
     * 内置 MCP（builtIn=1）不可删除。
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        mcpToolService.delete(id);
        return ApiResult.success();
    }

    /**
     * 查询指定 MCP 服务下的工具列表
     * <p>
     * 场景 5：查看单个 MCP 服务的工具详情。
     * 数据来源：OwnerToolBindingContext 缓存。
     */
    @GetMapping("/{id}/tools")
    public ApiResult<List<McpToolItemVO>> listTools(@PathVariable("id") Long id) {
        return ApiResult.success(mcpToolService.listToolsByMcpServer(id));
    }

    /**
     * 手动同步 MCP 服务器工具
     * <p>
     * 场景 6：用户手动触发重新连接 MCP 协议，重新拉取远端工具列表并同步到 DB。
     * 连接失败时自动禁用该 MCP 服务，返回具体错误信息。
     */
    @PostMapping("/{id}/sync-tools")
    public ApiResult<McpConnectResult> syncTools(@PathVariable("id") Long id) {
        return ApiResult.success(mcpToolService.syncTools(id));
    }

    /**
     * 编辑 MCP 服务器（全量覆盖更新）
     * <p>
     * 保存时根据 enabled 状态自动连接拉取工具列表。
     *
     * @param id      MCP 服务器 ID
     * @param request 编辑参数
     * @return 更新后的 MCP 服务 VO
     */
    @PutMapping("/{id}")
    public ApiResult<McpServerVO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody McpServerUpdateRequest request) {
        request.setId(id);
        return ApiResult.success(mcpToolService.update(request));
    }

    /**
     * 内置工具启用/禁用切换
     * <p>
     * 内置工具不涉及 MCP 协议连接，仅更新工具本身的 enabled 状态并刷新缓存。
     */
    @PatchMapping("/built-in/status")
    public ApiResult<Void> updateBuiltInStatus(@Valid @RequestBody BuiltInStatusUpdateRequest request) {
        mcpToolService.updateBuiltInStatus(request);
        return ApiResult.success();
    }

    /**
     * 测试 MCP 服务器连接
     * <p>
     * 仅测试连通性，返回远端工具列表，不会写入数据库，不会改变 MCP 的启用/禁用状态。
     *
     * @param request MCP 服务器连接参数（无需 ID）
     * @return 连接结果（含工具列表）
     */
    @PostMapping("/test-connection")
    public ApiResult<McpConnectResult> testConnection(@Valid @RequestBody McpServerCreateRequest request) {
        return ApiResult.success(mcpToolService.testConnection(request));
    }
}
