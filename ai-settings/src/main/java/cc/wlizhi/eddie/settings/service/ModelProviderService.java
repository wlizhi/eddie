/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.settings.entity.request.ModelProviderCreateRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelProviderUpdateRequest;
import cc.wlizhi.eddie.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;

import java.util.List;

/**
 * 模型提供服务
 */
public interface ModelProviderService {

    /**
     * 查询所有服务提供商列表（按 sort_order 和 code 排序）
     */
    List<ModelProviderVO> listAll();

    /**
     * 查询所有服务商及其模型列表（两层嵌套结构）
     */
    List<ModelProviderVO> listWithModels();

    /**
     * 根据服务商 code 获取模型列表
     */
    List<ModelVO> getModelsByCode(String code);

    /**
     * 新增服务提供商
     */
    void create(ModelProviderCreateRequest request);

    /**
     * 修改服务提供商
     */
    void update(ModelProviderUpdateRequest request);

    /**
     * 根据 id 删除服务提供商
     */
    void deleteById(Long id);

    /**
     * 全量更新排序序号（前端拖拽后按顺序传入 id 数组）
     */
    void updateSortOrder(List<Long> orderedIds);

    /**
     * 根据服务商 id 远程拉取模型列表
     */
    List<ModelVO> fetchRemoteModels(Long providerId);
}
