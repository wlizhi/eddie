/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.settings.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 修改服务商请求参数
 */
@Setter
@Getter
public class ModelProviderUpdateRequest {

    /**
     * 主键 ID
     */
    @NotNull(message = "服务商 ID 不能为空")
    private Long id;

    /**
     * 显示名称（内置服务商不可修改）
     */
    private String name;

    /**
     * API 基础地址
     */
    private String baseUrl;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 该实例下的模型 ID 列表 JSON 字符串
     */
    private String models;

    /**
     * 0=禁用, 1=启用
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
