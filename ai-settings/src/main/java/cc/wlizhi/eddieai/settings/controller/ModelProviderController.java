package cc.wlizhi.eddieai.settings.controller;

import cc.wlizhi.eddieai.common.dto.ApiResult;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;
import cc.wlizhi.eddieai.settings.service.ModelProviderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型提供商
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
    public ApiResult<Void> create(@RequestBody ModelProviderEntity entity) {
        modelProviderService.create(entity);
        return ApiResult.success();
    }

    /**
     * 修改服务提供商
     */
    @PutMapping
    public ApiResult<Void> update(@RequestBody ModelProviderEntity entity) {
        modelProviderService.update(entity);
        return ApiResult.success();
    }

    /**
     * 删除服务提供商
     */
    @DeleteMapping("/{code}")
    public ApiResult<Void> deleteByCode(@PathVariable String code) {
        modelProviderService.deleteByCode(code);
        return ApiResult.success();
    }
}
