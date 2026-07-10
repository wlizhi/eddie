/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.controller;

import cc.wlizhi.eddie.agent.entity.request.AgentCreateRequest;
import cc.wlizhi.eddie.agent.entity.request.AgentUpdateRequest;
import cc.wlizhi.eddie.agent.entity.response.AgentDetailVO;
import cc.wlizhi.eddie.agent.entity.response.AgentVO;
import cc.wlizhi.eddie.agent.service.AgentService;
import cc.wlizhi.eddie.chat.entity.response.ToolSourceVO;
import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 智能体管理接口
 * 智能体管理接口
 * <p>
 * 提供智能体的增删改查、批量排序等管理功能。
 * 与 {@code /api/agent/chat} 区分，管理路径为 {@code /api/agent/manage}。
 */
@RestController
@RequestMapping("/api/agent/manage")
public class AgentController {

    @Resource
    private AgentService agentService;

    /**
     * 查询智能体列表
     *
     * @param showAll true=查询全部, false=仅查询启用的（默认 false）
     */
    @GetMapping("/list")
    public ApiResult<List<AgentVO>> list(@RequestParam(name = "showAll", defaultValue = "false") boolean showAll) {
        return ApiResult.success(agentService.list(showAll));
    }

    /**
     * 获取智能体详情（配置回显）
     *
     * @param id 智能体 ID
     */
    @GetMapping("/{id}")
    public ApiResult<AgentDetailVO> getDetail(@PathVariable(name = "id") Long id) {
        return ApiResult.success(agentService.getDetail(id));
    }

    /**
     * 新建智能体
     */
    @PostMapping
    public ApiResult<AgentVO> create(@Valid @RequestBody AgentCreateRequest request) {
        return ApiResult.success(agentService.create(request));
    }

    /**
     * 更新智能体设置
     *
     * @param id      智能体 ID
     * @param request 更新参数
     */
    @PutMapping("/{id}")
    public ApiResult<AgentVO> update(@PathVariable(name = "id") Long id,
                                     @Valid @RequestBody AgentUpdateRequest request) {
        return ApiResult.success(agentService.update(id, request));
    }

    /**
     * 删除智能体（级联删除关联的会话和消息）
     *
     * @param id 智能体 ID
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable(name = "id") Long id) {
        agentService.delete(id);
        return ApiResult.success();
    }

    /**
     * 获取智能体已绑定的 MCP 工具列表（二层结构：MCP → tools）
     * <p>
     * 仅返回当前智能体已绑定的 MCP Server 及其下辖工具。
     * 按 MCP sort_order 排序，供输入框手动模式选择 MCP 使用。
     *
     * @param id 智能体 ID
     */
    @GetMapping("/{id}/mcp-tools")
    public ApiResult<List<ToolSourceVO>> getBoundMcpTools(@PathVariable(name = "id") Long id) {
        return ApiResult.success(agentService.getBoundMcpTools(id));
    }

    /**
     * 更新智能体头像（支持文字、emoji、图片上传）
     *
     * @param id         智能体 ID
     * @param avatarText 文字或 emoji（可选）
     * @param file       图片文件（可选）
     */
    @PostMapping("/{id}/avatar")
    public ApiResult<AgentVO> updateAvatar(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "avatar", required = false) String avatarText,
            @RequestParam(name = "file", required = false) MultipartFile file) {
        return ApiResult.success(agentService.updateAvatar(id, avatarText, file));
    }

    /**
     * 批量排序：按 ID 数组顺序重新赋 sort_order（1,2,3...）
     *
     * @param ids 排序后的智能体 ID 列表
     */
    @PutMapping("/batch-sort")
    public ApiResult<Void> batchSort(@RequestBody List<Long> ids) {
        agentService.batchSort(ids);
        return ApiResult.success();
    }
}
