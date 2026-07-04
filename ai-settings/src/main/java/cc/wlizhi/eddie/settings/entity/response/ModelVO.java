/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.settings.entity.response;

import cc.wlizhi.eddie.common.enums.ModelCapability;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 模型信息响应 VO（接口2：根据服务商 code 获取模型列表）
 */
@Getter
@Setter
public class ModelVO {

    /** 对应 JSON 中的 id 字段 */
    private String code;

    /** 对应 JSON 中的 object 字段 */
    private String object;

    /** 对应 JSON 中的 owned_by 字段 */
    private String ownedBy;

    /**
     * 对应 JSON 中的 created 字段（Unix 时间戳）
     */
    private Long created;

    /**
     * 模型能力标签列表，如 [vision, function_calling, reasoning]
     */
    private List<ModelCapability> capabilities;

    /**
     * 币种，如 USD、CNY
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

    /**
     * 缓存命中价格，每百万 token
     */
    private Double cacheInputPrice;

    /**
     * 缓存写入价格，每百万 token
     */
    private Double cacheWriteInputPrice;

    /**
     * 调用间隔（秒），模型每次调用的最小时间间隔<br>
     * null 或 0 表示不限制
     */
    private Integer callIntervalSec;

}
