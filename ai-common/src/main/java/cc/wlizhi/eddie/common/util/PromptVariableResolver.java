/**
 * @author Eddie
 * {@code @date} 2026-06-28
 */

package cc.wlizhi.eddie.common.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Supplier;

/**
 * 系统提示词模板变量解析器
 * <p>
 * 支持在 system prompt 中使用 {@code ${variable}} 语法嵌入动态系统变量。
 * 变量分为两类：
 * <ul>
 *   <li><b>内置变量</b>：由系统环境决定，无需额外参数即可解析</li>
 *   <li><b>额外变量</b>：调用方通过 {@link #resolve(String, Map)} 传入，优先级高于同名内置变量</li>
 * </ul>
 * <p>
 * 使用 {@link PropertyPlaceholderHelper} 进行占位符替换，支持：
 * <ul>
 *   <li>{@code ${ key }}（占位符内允许空格，自动 trim）</li>
 *   <li>{@code ${key:默认值}}（未注册变量时回退到默认值）</li>
 *   <li>{@code ${unknown}}（未注册且无默认值时，原样保留）</li>
 * </ul>
 * <p>
 * 前端可通过 {@code GET /api/system/prompt-variables} 动态获取支持的变量列表。
 */
@Component
public class PromptVariableResolver {

    /**
     * 从系统获取真实时区，支持夏令时自动偏移。
     * 个人桌面助手跨时区使用时，无需硬编码时区。
     */
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DTF_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DTF_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DTF_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Spring 属性占位符解析器
     * <ul>
     *   <li>前缀：{@code ${}</li>
     *   <li>后缀：{@code }}</li>
     *   <li>默认值分隔符：{@code :}</li>
     *   <li>忽略未解析占位符：{@code true}（保留原样，不抛异常）</li>
     * </ul>
     */
    private static final PropertyPlaceholderHelper HELPER =
            new PropertyPlaceholderHelper("${", "}", ":", null, true);

    /**
     * 变量元数据，供前端展示
     */
    @Getter
    public static class VariableInfo {
        /**
         * 变量名，如 "datetime"
         */
        private final String key;

        /**
         * 模板字符串，如 "${datetime}"
         */
        private final String template;

        /**
         * 示例值，如 "2026-06-28 19:57"
         */
        private final String example;

        /**
         * 描述，如 "当前日期和时间"
         */
        private final String description;

        public VariableInfo(String key, String example, String description) {
            this.key = key;
            this.template = "${" + key + "}";
            this.example = example;
            this.description = description;
        }
    }

    /**
     * 内置变量注册表：templateKey → valueSupplier
     */
    private final Map<String, Supplier<String>> builtinResolvers = new LinkedHashMap<>();

    /**
     * 前端可获取的变量列表
     */
    private final List<VariableInfo> variableInfos = new ArrayList<>();

    @PostConstruct
    public void init() {
        // ==================== 时间类 ====================
        register("datetime",
                () -> now().format(DTF_DATETIME),
                "当前日期和时间",
                "2026-06-28 19:57");
        register("date",
                () -> now().format(DTF_DATE),
                "当前日期",
                "2026-06-28");
        register("time",
                () -> now().format(DTF_TIME),
                "当前时间",
                "19:57");
        register("weekday",
                () -> now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA),
                "当前星期",
                "星期日");
        register("timezone",
                () -> {
                    var offset = ZONE.getRules().getOffset(Instant.now());
                    return ZONE + " (UTC" + offset.getId() + ")";
                },
                "时区信息",
                "Asia/Shanghai (UTC+8)");
        register("language",
                () -> Locale.getDefault().toLanguageTag(),
                "系统语言",
                "zh-CN");

        // ==================== 系统环境（单变量） ====================
        register("os",
                () -> System.getProperty("os.name") + " " + System.getProperty("os.version"),
                "操作系统名称及版本",
                "macOS Sonoma 14.5");
        register("cpu_arch",
                () -> {
                    String arch = System.getProperty("os.arch");
                    return "aarch64".equals(arch) ? "Apple Silicon (arm64)" : arch;
                },
                "CPU 架构",
                "Apple Silicon (arm64)");
        register("memory_size",
                PromptVariableResolver::getTotalMemory,
                "物理内存大小",
                "18 GB");

        // ==================== 系统信息合集 ====================
        register("system_info",
                PromptVariableResolver::getSystemInfoBlock,
                "系统信息合集（含操作系统、CPU 架构、物理内存）",
                "操作系统：macOS Sonoma 14.5\nCPU 架构：Apple Silicon (arm64)\n物理内存：18 GB");

        // ==================== 用户类 ====================
        register("username",
                () -> System.getProperty("user.name"),
                "用户名",
                "eddie");
    }

    /**
     * 解析模板变量 — 仅使用内置变量
     *
     * @param prompt 原始 system prompt，可包含 {@code ${variable}} 占位符
     * @return 替换后的 system prompt
     */
    public String resolve(String prompt) {
        return resolve(prompt, null);
    }

    /**
     * 解析模板变量 — 支持额外 KV
     * <p>
     * 执行顺序：先替换内置变量，再替换额外变量。
     * 额外变量优先级高于内置变量，同名 key 以额外变量值为准。
     *
     * @param prompt 原始 system prompt，可包含 {@code ${variable}} 占位符
     * @param extras 调用方传入的额外变量（可为 null），key 不需要 {@code ${}} 包裹
     * @return 替换后的 system prompt
     */
    public String resolve(String prompt, Map<String, String> extras) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }

        // 合并所有变量（内置 + 额外）
        Map<String, String> allValues = new HashMap<>();
        for (var entry : builtinResolvers.entrySet()) {
            // entry key 格式是 "${datetime}"，去掉 ${} 外壳保留 key 名
            String key = entry.getKey().substring(2, entry.getKey().length() - 1);
            allValues.put(key, entry.getValue().get());
        }
        if (extras != null) {
            // 额外变量覆盖同名内置变量
            allValues.putAll(extras);
        }

        return HELPER.replacePlaceholders(prompt, allValues::get);
    }

    /**
     * 获取所有支持的变量列表（供前端 {@code GET /api/system/prompt-variables} 使用）
     * <p>
     * 新增变量时只需在 {@link #init()} 中添加一行 {@link #register} 调用，
     * 此方法自动返回新变量，前端无需改动。
     *
     * @return 不可变的变量信息列表
     */
    public List<VariableInfo> getSupportedVariables() {
        return Collections.unmodifiableList(variableInfos);
    }

    // ==================== 内部方法 ====================

    /**
     * 注册一个内置变量
     *
     * @param key         变量名（不含 ${}）
     * @param resolver    值提供者
     * @param description 描述
     * @param example     示例值
     */
    private void register(String key, Supplier<String> resolver, String description, String example) {
        builtinResolvers.put("${" + key + "}", resolver);
        variableInfos.add(new VariableInfo(key, example, description));
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /**
     * 获取物理内存大小（GB）
     * <p>
     * 优先使用 {@link com.sun.management.OperatingSystemMXBean#getTotalMemorySize()} 获取真实物理内存，
     * 失败时回退到 JVM 最大堆内存。
     */
    private static String getTotalMemory() {
        try {
            var osBean = (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();
            long bytes = osBean.getTotalMemorySize();
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 生成系统信息合集块
     * <p>
     * 输出格式：
     * <pre>
     * 操作系统：macOS Sonoma 14.5
     * CPU 架构：Apple Silicon (arm64)
     * 物理内存：18 GB
     * </pre>
     */
    private static String getSystemInfoBlock() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String arch = System.getProperty("os.arch");
        String cpu = "aarch64".equals(arch) ? "Apple Silicon (arm64)" : arch;
        return "操作系统：" + os + "\nCPU 架构：" + cpu + "\n物理内存：" + getTotalMemory();
    }
}
