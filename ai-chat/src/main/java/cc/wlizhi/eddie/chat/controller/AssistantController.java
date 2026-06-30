/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.controller;

import cc.wlizhi.eddie.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddie.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddie.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddie.chat.entity.response.AssistantVO;
import cc.wlizhi.eddie.chat.entity.response.ToolSourceVO;
import cc.wlizhi.eddie.chat.service.AssistantService;
import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 助手列表接口
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Resource
    private AssistantService assistantService;

    /**
     * 查询助手列表
     *
     * @param showAll true=查询全部, false=仅查询启用的（默认 false）
     */
    @GetMapping("/list")
    public ApiResult<List<AssistantVO>> list(@RequestParam(name = "showAll", defaultValue = "false") boolean showAll) {
        return ApiResult.success(assistantService.list(showAll));
    }

    /**
     * 获取助手详情（配置回显）
     */
    @GetMapping("/{id}")
    public ApiResult<AssistantDetailVO> getDetail(@PathVariable(name = "id") Long id) {
        return ApiResult.success(assistantService.getDetail(id));
    }

    /**
     * 新建助手
     */
    @PostMapping
    public ApiResult<AssistantVO> create(@Valid @RequestBody AssistantCreateRequest request) {
        return ApiResult.success(assistantService.create(request));
    }

    /**
     * 更新助手设置
     */
    @PutMapping("/{id}")
    public ApiResult<AssistantVO> update(@PathVariable(name = "id") Long id,
                                         @Valid @RequestBody AssistantUpdateRequest request) {
        return ApiResult.success(assistantService.update(id, request));
    }

    /**
     * 更新助手头像（支持文字、emoji、图片上传）
     * <p>
     * Controller 仅接收参数转发，业务逻辑在 Service 层。
     *
     * @param id         助手 ID
     * @param avatarText 文字或 emoji（可选）
     * @param file       图片文件（可选）
     */
    @PostMapping("/{id}/avatar")
    public ApiResult<AssistantVO> updateAvatar(
            @PathVariable(name = "id") Long id,
            @RequestParam(value = "avatar", required = false) String avatarText,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ApiResult.success(assistantService.updateAvatar(id, avatarText, file));
    }

    /**
     * 删除助手
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable(name = "id") Long id) {
        assistantService.delete(id);
        return ApiResult.success();
    }

    /**
     * 批量排序：按 ID 数组顺序重新赋 sort_order（1,2,3...）
     */
    @PutMapping("/batch-sort")
    public ApiResult<Void> batchSort(@RequestBody List<Long> ids) {
        assistantService.batchSort(ids);
        return ApiResult.success();
    }

    /**
     * 获取助手已绑定的 MCP 工具列表（二层结构：MCP → tools）
     * <p>
     * 仅返回当前助手已绑定的 MCP Server 及其下辖工具。
     * 用于输入框手动模式选择 MCP 使用。
     *
     * @param id 助手 ID
     */
    @GetMapping("/{id}/mcp-tools")
    public ApiResult<List<ToolSourceVO>> getBoundMcpTools(@PathVariable("id") Long id) {
        return ApiResult.success(assistantService.getBoundMcpTools(id));
    }

}
