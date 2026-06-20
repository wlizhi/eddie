package cc.wlizhi.eddieai.chat.controller;

import cc.wlizhi.eddieai.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddieai.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddieai.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddieai.chat.entity.response.AssistantVO;
import cc.wlizhi.eddieai.chat.service.AssistantService;
import cc.wlizhi.eddieai.common.dto.ApiResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
                                         @RequestBody AssistantUpdateRequest request) {
        return ApiResult.success(assistantService.update(id, request));
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
}
