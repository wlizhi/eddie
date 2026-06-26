package cc.wlizhi.eddie.settings.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.settings.service.McpBindingService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * MCP 绑定关系管理 API
 * <p>
 * 以 MCP Server 为粒度，对指定 Owner（助手/智能体）进行绑定关系的新增或移除。
 * 每次操作只处理一个 MCP Server 下的所有工具。
 * <p>
 * 使用示例：
 * <pre>
 * POST   /api/mcp-servers/5/bindings?ownerType=ASSISTANT&ownerId=3
 * DELETE /api/mcp-servers/5/bindings?ownerType=ASSISTANT&ownerId=3
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/mcp-servers")
public class McpBindingController {

    @Resource
    private McpBindingService mcpBindingService;

    /**
     * 新增绑定：将指定 MCP Server 下的所有工具绑定给 Owner
     * <p>
     * 校验 MCP 存在 → 获取工具列表 → 批量写入绑定表 → 刷新缓存。
     *
     * @param mcpServerId MCP Server ID（路径变量）
     * @param ownerType   归属方类型（ASSISTANT / AGENT）
     * @param ownerId     归属方 ID
     */
    @PostMapping("/{mcpServerId}/bindings")
    public ApiResult<Void> addBinding(
            @PathVariable("mcpServerId") @NotNull Long mcpServerId,
            @RequestParam("ownerType") @NotBlank String ownerType,
            @RequestParam("ownerId") @NotNull Long ownerId) {
        mcpBindingService.addBinding(RoleType.valueOf(ownerType.toUpperCase()), ownerId, mcpServerId);
        return ApiResult.success();
    }

    /**
     * 移除绑定：将指定 MCP Server 下的所有工具从 Owner 解绑
     * <p>
     * 获取工具列表 → 按工具 ID 删除绑定记录 → 刷新缓存。
     *
     * @param mcpServerId MCP Server ID（路径变量）
     * @param ownerType   归属方类型（ASSISTANT / AGENT）
     * @param ownerId     归属方 ID
     */
    @DeleteMapping("/{mcpServerId}/bindings")
    public ApiResult<Void> removeBinding(
            @PathVariable("mcpServerId") @NotNull Long mcpServerId,
            @RequestParam("ownerType") @NotBlank String ownerType,
            @RequestParam("ownerId") @NotNull Long ownerId) {
        mcpBindingService.removeBinding(RoleType.valueOf(ownerType.toUpperCase()), ownerId, mcpServerId);
        return ApiResult.success();
    }
}
