package cc.wlizhi.eddieai.settings.service;

import cc.wlizhi.eddieai.settings.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.settings.entity.response.ModelProviderVO;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;

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
     * 根据服务商 code 获取模型列表
     */
    List<ModelVO> getModelsByCode(String code);

    /**
     * 新增服务提供商
     */
    void create(ModelProviderEntity entity);

    /**
     * 修改服务提供商
     */
    void update(ModelProviderEntity entity);

    /**
     * 删除服务提供商
     */
    void deleteByCode(String code);
}
