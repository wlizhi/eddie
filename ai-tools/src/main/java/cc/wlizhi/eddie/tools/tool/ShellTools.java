/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.dto.ConfigFieldDescriptor;
import cc.wlizhi.eddie.common.dto.ConfigSchema;
import cc.wlizhi.eddie.common.dto.ShellToolConfig;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 内置 Shell 命令执行工具。<p>
 * 允许 AI 模型在本地系统执行 shell 命令并获取输出结果。<br>
 * 权限模式（通过设置页面配置）：
 * <ul>
 *   <li><b>SMART（默认）</b> — 只读命令自动放行，写入命令需确认，高危命令直接拒绝</li>
 *   <li><b>CUSTOM</b> — 自定义黑白名单，黑名单优先拦截</li>
 *   <li><b>PERMISSIVE</b> — 全部放行，无限制</li>
 * </ul>
 * 配置存储在对应 MCP Server 的 {@code source_config} 字段中。
 */
@Component
public class ShellTools implements BuiltInToolProvider {

    @Override
    public String getMcpServerName() {
        return "BuiltInShell";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        ConfigFieldDescriptor blacklistField = new ConfigFieldDescriptor(
                "blacklist", "textarea", "黑名单",
                "每行一个命令前缀，命中直接拒绝（仅 CUSTOM 模式生效）",
                "rm\ndd\nmkfs\nfdisk\nsudo", null, false, null, null, null,
                null, null
        );
        blacklistField.setDependsOn("mode");
        blacklistField.setDependsOnValue("CUSTOM");

        ConfigFieldDescriptor whitelistField = new ConfigFieldDescriptor(
                "whitelist", "textarea", "白名单",
                "每行一个命令前缀，仅允许这些命令（仅 CUSTOM 模式生效）",
                "ls\ncat\necho\npwd", null, false, null, null, null,
                null, null
        );
        whitelistField.setDependsOn("mode");
        whitelistField.setDependsOnValue("CUSTOM");

        return new ConfigSchema("built_in_shell_exec", "Shell 权限设置", "控制 AI 在本地执行 shell 命令的权限范围",
                List.of(
                        new ConfigFieldDescriptor(
                                "mode", "select", "权限模式",
                                "SMART：只读自动放行 / 写入需确认；CUSTOM：手动黑白名单；PERMISSIVE：全部放行",
                                "SMART", null, false, null, null,
                                List.of(
                                        new ConfigFieldDescriptor.SelectOption("SMART", "智能模式（推荐）"),
                                        new ConfigFieldDescriptor.SelectOption("CUSTOM", "自定义模式"),
                                        new ConfigFieldDescriptor.SelectOption("PERMISSIVE", "宽松模式")
                                ),
                                null, null
                        ),
                        new ConfigFieldDescriptor(
                                "maxOutputChars", "number", "最大输出字符数",
                                "限制命令输出的最大字符数（100 ~ 10000），超长输出将被截断",
                                10000, null, false, 100, 10000,
                                null, null, null
                        ),
                        blacklistField,
                        whitelistField
                ));
    }

    /** 此工具的 @Tool name，用于在 source_config 中按 namespace 读取配置 */
    private static final String MY_TOOL_NAME = "built_in_shell_exec";

    private static final Logger log = LoggerFactory.getLogger(ShellTools.class);

    /**
     * 输出最大字符数上限
     */
    private static final int MAX_OUTPUT_CHARS_LIMIT = 10_000;

    /**
     * 输出最大字符数下限
     */
    private static final int MIN_OUTPUT_CHARS_LIMIT = 100;

    /**
     * 默认超时时间（秒），后续可改为通过配置读取
     */
    private static final int TIMEOUT_SECONDS = 30;

    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "built_in_shell_exec",
            description = """
                    在本地系统执行 shell 命令，返回标准输出和错误输出。
                    
                    **适用场景：**
                    - 文件操作（ls、cp、mv、cat 等）
                    - 系统信息查询（ps、df、uname 等）
                    - 脚本执行
                    - 其他命令行操作
                    
                    **注意事项：**
                    - 命令会在用户电脑上直接执行，请确认命令安全
                    - 输出长度上限：10,000 字符（可在设置中调整）
                    - 建议优先使用只读命令（ls、cat、echo、pwd 等），避免执行危险操作
                    """)
    public ApiResult<String> exec(
            @ToolParam(description = "要执行的 shell 命令，如 'ls -la /tmp' 或 'echo hello'") String command) {

        if (command == null || command.isBlank()) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "命令不能为空");
        }

        // 1. 读取权限配置
        ShellToolConfig config = loadConfig();

        // 2. 计算有效最大输出字符数
        int maxOutputChars = resolveMaxOutputChars(config);

        // 3. 权限校验
        ApiResult<String> permissionResult = checkPermission(command, config);
        if (permissionResult != null) {
            return permissionResult;
        }

        log.info("[ShellTools] 执行命令: {}", command);

        try {
            // 根据操作系统选择合适的 shell
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isWindows = osName.contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("cmd.exe", "/c", command)
                    : new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("[ShellTools] 命令超时已强制终止: {}", command);
                return ApiResult.error(ApiResultCode.TIMEOUT,
                        "命令执行超时（" + TIMEOUT_SECONDS + "秒），已强制终止进程");
            }

            int exitCode = process.exitValue();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() + line.length() + 1 > maxOutputChars) {
                        output.append("\n...（输出已截断，超出 ").append(maxOutputChars).append(" 字符）");
                        // 跳过剩余输入流
                        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                        break;
                    }
                    if (!output.isEmpty()) {
                        output.append("\n");
                    }
                    output.append(line);
                }
            }

            String result = output.toString();

            if (exitCode != 0) {
                log.warn("[ShellTools] 命令退出码非零: exitCode={}, command={}", exitCode, command);
                return ApiResult.success(
                        "命令执行完成（退出码: " + exitCode + "）\n输出:\n" + result);
            }

            log.info("[ShellTools] 命令执行成功: exitCode=0, outputLength={}", result.length());
            return ApiResult.success(result.isEmpty() ? "命令执行成功，无输出" : result);

        } catch (Exception e) {
            log.error("[ShellTools] 命令执行异常: {}", command, e);
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR,
                    "命令执行失败: " + e.getMessage());
        }
    }

    /**
     * 从 MCP Server 的 source_config 中加载 Shell 权限配置。<p>
     * 新格式：{ "built_in_shell_exec": { "mode": "SMART", ... } }<br>
     * 旧格式兼容：{ "mode": "SMART", ... }（扁平结构）
     *
     * @return 解析后的配置，解析失败返回默认配置（SMART 模式）
     */
    private ShellToolConfig loadConfig() {
        McpServerEntity server = ownerToolBindingContext.getBuiltInMcpServerByName(getMcpServerName());
        if (server == null) {
            return new ShellToolConfig();
        }
        String sourceConfig = server.getSourceConfig();
        if (sourceConfig == null || sourceConfig.isBlank() || "{}".equals(sourceConfig)) {
            return new ShellToolConfig();
        }
        try {
            JsonNode root = objectMapper.readTree(sourceConfig);
            JsonNode myConfig = root.get(MY_TOOL_NAME);
            if (myConfig != null && !myConfig.isNull() && !myConfig.isEmpty()) {
                // 新格式：按 tool name 命名空间读取
                return objectMapper.treeToValue(myConfig, ShellToolConfig.class);
            }
            // 兼容旧格式：整个 sourceConfig 就是该工具的配置
            return objectMapper.treeToValue(root, ShellToolConfig.class);
        } catch (Exception e) {
            log.warn("[ShellTools] 解析 sourceConfig 失败，使用默认配置: {}", e.getMessage());
            return new ShellToolConfig();
        }
    }

    /**
     * 解析有效最大输出字符数。<p>
     * 用户可配置 100~10000，未配置则返回默认值 10000。
     *
     * @param config Shell 工具配置
     * @return 有效的最大输出字符数
     */
    private static int resolveMaxOutputChars(ShellToolConfig config) {
        Integer configured = config.getMaxOutputChars();
        if (configured == null) {
            return MAX_OUTPUT_CHARS_LIMIT;
        }
        return Math.clamp(configured, MIN_OUTPUT_CHARS_LIMIT, MAX_OUTPUT_CHARS_LIMIT);
    }

    /**
     * 权限校验。<p>
     * 根据配置的模式进行校验：
     * <ul>
     *   <li>PERMISSIVE — 不校验，返回 null</li>
     *   <li>SMART — 只读自动放行，高危直接拒绝，写入需确认</li>
     *   <li>CUSTOM — 黑名单优先拦截，再检查白名单</li>
     * </ul>
     *
     * @param command 待执行的 shell 命令
     * @param config  权限配置
     * @return 校验不通过时返回错误 ApiResult，通过时返回 null
     */
    private ApiResult<String> checkPermission(String command, ShellToolConfig config) {
        String mode = config.getMode();
        if (mode == null) {
            mode = "SMART";
        }

        // PERMISSIVE 模式：全部放行
        if ("PERMISSIVE".equals(mode)) {
            return null;
        }

        // SMART 模式：基于内置命令分类
        if ("SMART".equals(mode)) {
            // 高危命令 → 直接拒绝
            if (ShellToolConfig.isDangerous(command)) {
                log.warn("[ShellTools] SMART 模式拦截高危命令: {}", command);
                return ApiResult.error(ApiResultCode.COMMAND_NOT_PERMITTED,
                        "检测到高危命令，已拦截: " + command);
            }
            // 写入操作 → 需要用户确认
            if (ShellToolConfig.isWriteOperation(command)) {
                log.warn("[ShellTools] SMART 模式检测到写入操作，需确认: {}", command);
                return ApiResult.error(ApiResultCode.NEED_CONFIRMATION,
                        "检测到写入操作，请在页面确认后重试: " + command);
            }
            // 只读操作 → 自动放行
            return null;
        }

        // CUSTOM 模式：基于黑白名单
        if ("CUSTOM".equals(mode)) {
            String trimmed = command.trim();

            // 黑名单优先：命中即拒绝
            List<String> blacklist = config.getBlacklist();
            if (blacklist != null) {
                boolean blocked = blacklist.stream()
                        .map(String::trim)
                        .filter(p -> !p.isBlank())
                        .anyMatch(trimmed::startsWith);
                if (blocked) {
                    log.warn("[ShellTools] CUSTOM 模式黑名单拦截: {}", command);
                    return ApiResult.error(ApiResultCode.COMMAND_NOT_PERMITTED,
                            "命令在黑名单中，已拦截。允许的前缀: " +
                                    String.join(", ", config.getWhitelist() != null ? config.getWhitelist() : List.of()));
                }
            }

            // 白名单校验
            List<String> whitelist = config.getWhitelist();
            if (whitelist != null && !whitelist.isEmpty()) {
                boolean permitted = whitelist.stream()
                        .map(String::trim)
                        .filter(p -> !p.isBlank())
                        .anyMatch(trimmed::startsWith);
                if (!permitted) {
                    log.warn("[ShellTools] CUSTOM 模式白名单拒绝: {}", command);
                    return ApiResult.error(ApiResultCode.COMMAND_NOT_PERMITTED,
                            "命令不在白名单中，允许的前缀: " +
                                    String.join(", ", whitelist));
                }
            }

            return null; // 通过校验
        }

        // 未知模式，按 SMART 处理
        log.warn("[ShellTools] 未知权限模式: {}，按 SMART 模式处理", mode);
        return null;
    }
}
