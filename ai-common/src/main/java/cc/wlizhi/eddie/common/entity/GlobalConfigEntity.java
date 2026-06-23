package cc.wlizhi.eddie.common.entity;

import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import lombok.Getter;
import lombok.Setter;

/**
 * 全局配置表映射实体。<p>
 * config_key 使用枚举名 {@link GlobalConfigKey#name()} 存储。<br>
 * config_val 为 JSON 字符串，业务按需反序列化。
 *
 * @author Eddie
 */
@Setter
@Getter
public class GlobalConfigEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 配置键（枚举名），如 DEFAULT_MODEL
     */
    private String configKey;

    /**
     * 配置值（JSON 字符串）
     */
    private String configVal;

    /**
     * 描述
     */
    private String description;

    /**
     * 更新时间
     */
    private String updatedAt;
}
