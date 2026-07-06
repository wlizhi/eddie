package cc.wlizhi.eddie.common.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 常规设置 DTO，对应 {@link cc.wlizhi.eddie.common.enums.GlobalConfigKey#GENERAL_SETTINGS} 的 JSON 结构。<p>
 * 字段与前端 GeneralPanel.vue 保持一致。
 *
 * @author Eddie
 * {@code @date} 2026-07-04
 */
@Setter
@Getter
public class GeneralSettings {

    /**
     * 内置搜索工具每次返回的结果数量（1~20）
     */
    private int searchResultCount = 8;

    /**
     * 网页抓取最大字符数（1,000~15,000）
     */
    private int webFetchMaxChars = 8000;

    /**
     * 是否自动生成会话标题
     */
    private boolean enableAutoTitle = true;

    /**
     * 生成标题取前几轮对话（1~5）
     */
    private int titleGenerationRounds = 1;

    /**
     * 业务日志级别。<p>
     * 控制 {@code cc.wlizhi.eddie} 包及子包的日志输出级别。<br>
     * 可选值：TRACE / DEBUG / INFO / WARN / ERROR / OFF，空字符串表示不覆盖使用框架默认值。
     */
    private String logLevel = "";

}
