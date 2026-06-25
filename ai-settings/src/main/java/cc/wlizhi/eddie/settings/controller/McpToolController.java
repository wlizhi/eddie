package cc.wlizhi.eddie.settings.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.settings.entity.request.McpServerCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.McpStatusUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.McpServerVO;
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
     * 查询全量 MCP + 工具二层列表（含禁用）
     * <p>
     * 场景 1：全局设置页面展示已启用的 MCP 及工具。
     */
    @GetMapping
    public ApiResult<List<McpServerVO>> listAll() {
        return ApiResult.success(mcpToolService.listAll());
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
     */
    @PatchMapping("/status")
    public ApiResult<Void> updateStatus(@Valid @RequestBody McpStatusUpdateRequest request) {
        mcpToolService.updateStatus(request);
        return ApiResult.success();
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
}
