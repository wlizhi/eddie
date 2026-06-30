/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.settings.entity.request.ModelBatchAddRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelBatchRemoveRequest;
import cc.wlizhi.eddie.settings.entity.request.ModelUpdateRequest;

/**
 * 模型管理（单个模型维度：增/改/删）
 * <p>
 * 操作逻辑：根据服务商 id 查出 models JSON，按 code 找到目标模型进行操作，
 * 然后序列化回 JSON 存储到数据库。
 */
public interface ModelService {

    /**
     * 修改指定服务商下的某个模型参数
     */
    void updateModel(Long providerId, ModelUpdateRequest request);

    /**
     * 批量新增模型到指定服务商
     */
    void batchAddModels(Long providerId, ModelBatchAddRequest request);

    /**
     * 批量删除指定服务商下的模型
     */
    void batchRemoveModels(Long providerId, ModelBatchRemoveRequest request);
}
