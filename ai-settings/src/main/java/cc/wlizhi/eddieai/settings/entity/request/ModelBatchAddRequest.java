package cc.wlizhi.eddieai.settings.entity.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量新增模型请求
 */
@Setter
@Getter
public class ModelBatchAddRequest {

    /**
     * 要新增的模型列表
     */
    private List<BatchAddItem> models;

    @Setter
    @Getter
    public static class BatchAddItem {
        private String code;
        private String name;
        private String object;
        private String ownedBy;
        private List<String> capabilities;
        private String currency;
        private Double inputPrice;
        private Double outputPrice;
    }
}
