package cc.wlizhi.eddie.settings.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.settings.entity.request.ModelBatchAddRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelBatchRemoveRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelUpdateRequest;
import cc.wlizhi.eddie.settings.service.ModelService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 模型管理（单个模型维度 CRUD）
 * <p>
 * 对某个服务商下的模型列表进行增/改/删操作。
 * 逻辑：根据服务商 id 查出 models JSON，按 code 查找目标模型，修改后重新序列化存储。
 */
@RestController
@RequestMapping("/api/model-provider")
public class ModelController {

    @Resource
    private ModelService modelService;

    /**
     * 修改指定服务商下的某个模型参数
     * <p>
     * 根据 models JSON 中 id 字段匹配，找到后更新对应字段（仅更新传入的非 null 字段）。
     *
     * @param providerId 服务商 id
     * @param request    模型参数（code 用于匹配，其余字段按需更新）
     */
    @PutMapping("/{providerId}/model")
    public ApiResult<Void> updateModel(@PathVariable(name = "providerId") Long providerId,
                                       @RequestBody @Valid ModelUpdateRequest request) {
        modelService.updateModel(providerId, request);
        return ApiResult.success();
    }

    /**
     * 批量新增模型到指定服务商
     * <p>
     * 从远程拉取的模型列表中多选后调用此接口批量添加。
     * 已存在的模型 code 会被自动跳过。
     *
     * @param providerId 服务商 id
     * @param request    要新增的模型列表
     */
    @PostMapping("/{providerId}/models/batch-add")
    public ApiResult<Void> batchAddModels(@PathVariable(name = "providerId") Long providerId,
                                          @RequestBody ModelBatchAddRequest request) {
        modelService.batchAddModels(providerId, request);
        return ApiResult.success();
    }

    /**
     * 批量删除指定服务商下的模型
     * <p>
     * 根据模型 code 列表批量移除。
     *
     * @param providerId 服务商 id
     * @param request    要删除的模型 code 列表
     */
    @PostMapping("/{providerId}/models/batch-remove")
    public ApiResult<Void> batchRemoveModels(@PathVariable(name = "providerId") Long providerId,
                                             @RequestBody ModelBatchRemoveRequest request) {
        modelService.batchRemoveModels(providerId, request);
        return ApiResult.success();
    }
}
