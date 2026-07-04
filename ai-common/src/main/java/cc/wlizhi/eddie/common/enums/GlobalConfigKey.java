package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * 全局配置键枚举。<p>
 * DB 中 config_key 直接使用枚举名（{@link #name()}）存储。<br>
 * 缓存构建时遍历此枚举过滤 DB 数据，业务代码通过此枚举取值，避免魔法字符串。
 *
 * @author Eddie
 */
@Getter
public enum GlobalConfigKey {

    /**
     * 默认对话模型（创建助手时未指定模型则使用此配置）
     */
    DEFAULT_MODEL("默认对话模型", ConfigType.FRONTEND),

    /**
     * 快速模型（标题生成、中期记忆压缩、长期摘要等轻量杂活）
     */
    FAST_MODEL("快速模型", ConfigType.FRONTEND),

    /**
     * 翻译模型（翻译功能专用）
     */
    TRANSLATE_MODEL("翻译模型", ConfigType.FRONTEND),

    /**
     * 显示设置（字体大小、字体类型、主题模式等）
     * value 为 JSON: {"fontSize":"medium","fontFamily":"system","themeMode":"light","colorScheme":"blue"}
     */
    DISPLAY_SETTINGS("显示设置", ConfigType.FRONTEND),

    /**
     * 常规设置。<p>
     * value 为 JSON，包含字段：<br>
     * - searchResultCount: 搜索返回结果数量<br>
     * - webFetchMaxChars: 网页抓取最大字符数<br>
     * - enableAutoTitle: 是否自动生成会话标题<br>
     * - titleGenerationRounds: 生成标题取前几轮对话
     */
    GENERAL_SETTINGS("常规设置", ConfigType.FRONTEND),

    /**
     * 数据库初始化版本号（纯数字字符串，如 "0", "1", "2"）
     */
    DB_INIT_VERSION("数据库初始化版本号", ConfigType.BACKEND),

    /**
     * 工具调用响应最大长度（纯数字字符串，如 "5000"）
     */
    TOOL_CALL_MAX_LENGTH("工具调用响应最大长度", ConfigType.BACKEND),

    ;

    private final String description;
    private final ConfigType configType;

    GlobalConfigKey(String description, ConfigType configType) {
        this.description = description;
        this.configType = configType;
    }

}
