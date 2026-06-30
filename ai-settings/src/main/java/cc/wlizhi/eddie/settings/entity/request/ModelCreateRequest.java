/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.settings.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 为服务商新增单个模型参数
 */
@Setter
@Getter
public class ModelCreateRequest {

    /**
     * 模型标识（对应 JSON 中的 id 字段）
     */
    @NotBlank(message = "模型 code 不能为空")
    private String code;

    /**
     * 模型对象类型，如 "model"
     */
    private String object;

    /**
     * 所属组织
     */
    private String ownedBy;

    /**
     * 自定义模型名称
     */
    private String name;

    /**
     * 模型能力标签列表
     */
    private List<String> capabilities;

    /**
     * 币种
     */
    private String currency;

    /**
     * 输入价格，每百万 token
     */
    private Double inputPrice;

    /**
     * 输出价格，每百万 token
     */
    private Double outputPrice;
}
