package cc.wlizhi.eddie.settings.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 修改服务商下的单个模型参数
 */
@Setter
@Getter
public class ModelUpdateRequest {

    /**
     * 模型标识（对应 JSON 中的 id 字段），用于查找要更新的模型
     */
    @NotBlank(message = "模型 code 不能为空")
    private String code;

    /**
     * 自定义模型名称（不传则保持原有值）
     */
    private String name;

    /**
     * 模型能力标签列表（不传则保持原有值）
     */
    private List<String> capabilities;

    /**
     * 币种（不传则保持原有值）
     */
    private String currency;

    /**
     * 输入价格，每百万 token（不传则保持原有值）
     */
    private Double inputPrice;

    /**
     * 输出价格，每百万 token（不传则保持原有值）
     */
    private Double outputPrice;
}
