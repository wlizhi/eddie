package cc.wlizhi.eddieai.settings.entity.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量删除模型请求
 */
@Setter
@Getter
public class ModelBatchRemoveRequest {

    /**
     * 要删除的模型 code 列表
     */
    private List<String> codes;
}
