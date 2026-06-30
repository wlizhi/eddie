package cc.wlizhi.eddie.common.util;

/**
 * 配置工具类
 *
 * @author Eddie
 */
public class ConfigUtil {

    /**
     * 解析带范围限制的整数配置
     *
     * @param defaultValue 默认值
     * @param configValue  配置值（可能为 null 或空字符串）
     * @param minValue     绝对最小值（包含）
     * @param maxValue     绝对最大值（包含）
     * @return 解析后的值，确保在 [minValue, maxValue] 范围内
     */
    public static int resolveIntConfig(int defaultValue, String configValue, int minValue, int maxValue) {
        if (configValue == null || configValue.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(configValue.strip());
            return Math.clamp(value, minValue, maxValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}