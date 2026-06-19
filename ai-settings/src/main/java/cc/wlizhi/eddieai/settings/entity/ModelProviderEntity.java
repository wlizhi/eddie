package cc.wlizhi.eddieai.settings.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型服务提供商表映射实体
 */
@Setter
@Getter
public class ModelProviderEntity {

    /** 唯一代码，如 'openai', 'deepseek' */
    private String code;

    /** 显示名称 */
    private String name;

    /** API 基础地址 */
    private String baseUrl;

    /** API 密钥 */
    private String apiKey;

    /** 模型列表 JSON 字符串 */
    private String models;

    /** 0=禁用, 1=启用 */
    private Integer enabled;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

}
