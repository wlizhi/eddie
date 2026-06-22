package cc.wlizhi.eddieai.settings.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增服务商请求参数
 */
@Setter
@Getter
public class ModelProviderCreateRequest {

    /**
     * 业务类型代码，如 'custom-openai'
     */
    @NotBlank(message = "服务商 code 不能为空")
    private String code;

    /**
     * 显示名称
     */
    @NotBlank(message = "服务商名称不能为空")
    private String name;

    /**
     * API 基础地址
     */
    @NotBlank(message = "API 地址不能为空")
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
     * 0=用户自定义, 1=内置
     */
    private Integer builtIn;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
