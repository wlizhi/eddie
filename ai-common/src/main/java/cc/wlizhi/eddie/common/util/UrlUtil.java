package cc.wlizhi.eddie.common.util;

/**
 * URL 拼接工具类
 */
public final class UrlUtil {

    private UrlUtil() {
    }

    /**
     * 拼接 baseUrl 和 path，自动处理斜杠去重
     * <p>
     * 例如：
     * <ul>
     *   <li>{@code join("https://api.deepseek.com", "/models")} → {@code https://api.deepseek.com/models}</li>
     *   <li>{@code join("https://api.deepseek.com/", "/models")} → {@code https://api.deepseek.com/models}</li>
     *   <li>{@code join("https://api.deepseek.com/v1", "/models")} → {@code https://api.deepseek.com/v1/models}</li>
     *   <li>{@code join("https://api.deepseek.com", "models")} → {@code https://api.deepseek.com/models}</li>
     * </ul>
     */
    public static String join(String base, String path) {
        if (base == null || base.isBlank()) {
            return path;
        }
        if (path == null || path.isBlank()) {
            return base;
        }
        // 移除 base 末尾的 /
        String cleanBase = base.replaceAll("/+$", "");
        // 移除 path 开头的 /
        String cleanPath = path.replaceAll("^/+", "");
        return cleanBase + "/" + cleanPath;
    }
}
