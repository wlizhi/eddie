package cc.wlizhi.eddieai.settings.controller;

import cc.wlizhi.eddieai.common.dto.ApiResult;
import cc.wlizhi.eddieai.settings.entity.request.ModelProviderCreateRequest;
import cc.wlizhi.eddieai.settings.entity.request.ModelProviderUpdateRequest;
import cc.wlizhi.eddieai.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;
import cc.wlizhi.eddieai.settings.service.ModelProviderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型服务商管理
 */
@RestController
@RequestMapping("/api/model-provider")
public class ModelProviderController {

    @Resource
    private ModelProviderService modelProviderService;

    /**
     * 查询所有服务提供商列表（不分页）
     */
    @GetMapping("/list")
    public ApiResult<List<ModelProviderVO>> listAll() {
        return ApiResult.success(modelProviderService.listAll());
    }

    /**
     * 查询所有服务商及其模型列表（两层嵌套结构）
     */
    @GetMapping("/list-with-models")
    public ApiResult<List<ModelProviderVO>> listWithModels() {
        return ApiResult.success(modelProviderService.listWithModels());
    }

    /**
     * 根据服务商 code 获取模型列表
     */
    @GetMapping("/{code}/models")
    public ApiResult<List<ModelVO>> getModelsByCode(@PathVariable String code) {
        return ApiResult.success(modelProviderService.getModelsByCode(code));
    }

    /**
     * 新增服务提供商
     */
    @PostMapping
    public ApiResult<Void> create(@RequestBody @Valid ModelProviderCreateRequest request) {
        modelProviderService.create(request);
        return ApiResult.success();
    }

    /**
     * 修改服务提供商
     */
    @PutMapping
    public ApiResult<Void> update(@RequestBody @Valid ModelProviderUpdateRequest request) {
        modelProviderService.update(request);
        return ApiResult.success();
    }

    /**
     * 删除服务提供商
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        modelProviderService.deleteById(id);
        return ApiResult.success();
    }

    /**
     * 全量更新排序序号（前端拖拽后按顺序传入 id 数组）
     */
    @PutMapping("/sort-order")
    public ApiResult<Void> updateSortOrder(@RequestBody List<Long> orderedIds) {
        modelProviderService.updateSortOrder(orderedIds);
        return ApiResult.success();
    }
}
