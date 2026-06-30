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
    DEFAULT_MODEL("默认对话模型"),

    /**
     * 快速模型（标题生成、中期记忆压缩、长期摘要等轻量杂活）
     */
    FAST_MODEL("快速模型"),

    /**
     * 翻译模型（翻译功能专用）
     */
    TRANSLATE_MODEL("翻译模型"),

    /**
     * 显示设置（字体大小、字体类型、主题模式等）
     * value 为 JSON: {"fontSize":"medium","fontFamily":"system","themeMode":"light","colorScheme":"blue"}
     */
    DISPLAY_SETTINGS("显示设置"),

    /**
     * 数据库初始化版本号（纯数字字符串，如 "0", "1", "2"）
     */
    DB_INIT_VERSION("数据库初始化版本号"),

    /**
     * 内置搜索工具返回结果数量（纯数字字符串，如 "8"）
     */
    SEARCH_RESULT_COUNT("搜索返回结果数量"),

    /**
     * 网页抓取最大字符数（纯数字字符串，如 "8000"）
     */
    WEB_FETCH_MAX_CHARS("网页抓取最大字符数"),

    /**
     * 工具调用响应最大长度（纯数字字符串，如 "5000"）
     */
    TOOL_CALL_MAX_LENGTH("工具调用响应最大长度");

    private final String description;

    GlobalConfigKey(String description) {
        this.description = description;
    }

}
