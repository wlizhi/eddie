/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型服务提供商表映射实体
 */
@Setter
@Getter
public class ModelProviderEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 业务类型代码，如 'openai', 'deepseek'，用于匹配 ChatPolicy
     */
    private String code;

    /** 显示名称 */
    private String name;

    /** API 基础地址 */
    private String baseUrl;

    /** API 密钥 */
    private String apiKey;

    /** 该实例下的模型列表 JSON 字符串 */
    private String models;

    /** 0=禁用, 1=启用 */
    private Integer enabled;

    /**
     * 0=用户自定义(可删除/编辑), 1=内置(不可删除,可启/禁)
     */
    private Integer builtIn;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

}
