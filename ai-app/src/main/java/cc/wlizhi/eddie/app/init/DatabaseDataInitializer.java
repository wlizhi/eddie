package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.app.config.EddieProperties;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 版本化数据库初始化脚本执行器。<p>
 * 在 Spring Boot 完成 DDL 建表后执行，
 * 扫描 {@code classpath:db/init/*.sql} 文件，按文件名中的版本号 >
 * 当前已执行版本，顺序执行增量 SQL 脚本。
 *
 * <h3>文件名规则</h3>
 * <pre>
 *   model_provider_init_1.sql  → 版本 1
 *   settings_init-10.sql       → 版本 10
 * </pre>
 * 解析规则：取最后一个 {@code _} 或 {@code -} 到扩展名 {@code .} 之间的数字作为版本号。
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>查询 {@code global_config} 中 {@code DB_INIT_VERSION} 的值</li>
 *   <li>无记录 → 插入版本 {@code 0}</li>
 *   <li>扫描 classpath 下所有 {@code .sql} 文件，解析版本号</li>
 *   <li>筛选版本号 > 当前版本的脚本，按版本升序执行</li>
 *   <li>更新 {@code DB_INIT_VERSION} 为最大成功执行的版本号</li>
 * </ol>
 *
 * @author Eddie
 */
@Slf4j
@Component
public class DatabaseDataInitializer {

    private static final String VERSION_KEY = "DB_INIT_VERSION";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private EddieProperties eddieProperties;
    @Resource
    private InitScheduler initScheduler;

    @PostConstruct
    public void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 10, this::executePendingMigrations);
    }

    private void executePendingMigrations() {
        int currentVersion = getCurrentVersion();
        log.info("当前数据库初始化版本: {}", currentVersion);

        Map<Integer, String> pendingFiles = scanVersionedSqlFiles();
        if (pendingFiles.isEmpty()) {
            log.info("没有待执行的数据库初始化脚本");
            return;
        }

        // 筛选版本号 > 当前版本的脚本（TreeMap 保证按 version 升序）
        Map<Integer, String> toExecute = pendingFiles.entrySet().stream()
                .filter(e -> e.getKey() > currentVersion)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> b, TreeMap::new));

        if (toExecute.isEmpty()) {
            log.info("数据库已是最新版本 (v{})", currentVersion);
            return;
        }

        log.info("待执行的初始化脚本版本: {}", toExecute.keySet());

        int maxVersion = currentVersion;
        for (Map.Entry<Integer, String> entry : toExecute.entrySet()) {
            int version = entry.getKey();
            String sql = entry.getValue();
            try {
                log.info("执行数据库初始化脚本 v{}...", version);
                executeSqlScript(sql);
                maxVersion = Math.max(maxVersion, version);
                log.info("数据库初始化脚本 v{} 执行成功", version);
            } catch (Exception e) {
                log.error("数据库初始化脚本 v{} 执行失败: {}", version, e.getMessage());
            }
        }

        if (maxVersion > currentVersion) {
            updateVersion(maxVersion);
            log.info("数据库初始化版本已更新至 v{}", maxVersion);
        }
    }

    /**
     * 执行 SQL 脚本内容。<p>
     * 先移除所有 {@code --} 注释行，再按 {@code ;} 拆分逐条执行。
     */
    private void executeSqlScript(String script) {
        // 移除所有 -- 开头的注释行
        String cleaned = script.replaceAll("(?m)^--.*$", "");
        // 按分号拆分为单条语句
        String[] statements = cleaned.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            jdbcTemplate.execute(trimmed);
        }
    }

    /**
     * 获取当前已执行的数据库初始化版本号。
     * 如果 {@code global_config} 中不存在 {@code DB_INIT_VERSION} 记录，
     * 则插入版本 {@code 0} 并返回。
     */
    private int getCurrentVersion() {
        String sql = "SELECT config_val FROM global_config WHERE config_key = ?";
        var results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("config_val"), VERSION_KEY);
        if (results.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO global_config (config_key, config_val, description) VALUES (?, '0', '数据库初始化版本号')",
                    VERSION_KEY);
            return 0;
        }
        try {
            return Integer.parseInt(results.get(0));
        } catch (NumberFormatException e) {
            log.warn("全局配置 DB_INIT_VERSION 值异常: {}, 重置为 0", results.get(0));
            jdbcTemplate.update("UPDATE global_config SET config_val = '0' WHERE config_key = ?", VERSION_KEY);
            return 0;
        }
    }

    private Map<Integer, String> scanVersionedSqlFiles() {
        Map<Integer, String> result = new TreeMap<>();
        List<String> initScripts = eddieProperties.getInitScripts();
        for (String scriptPath : initScripts) {
            String filename = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);
            int version = parseVersionFromFilename(filename);
            if (version < 0) {
                log.debug("跳过不匹配的 SQL 文件: {}", filename);
                continue;
            }

            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + scriptPath);

            try (var reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sql = reader.lines().collect(Collectors.joining("\n")).trim();
                if (sql.isEmpty()) {
                    log.warn("初始化脚本 v{} 内容为空，跳过", version);
                    continue;
                }
                result.put(version, sql);
            } catch (Exception e) {
                log.error("加载初始化脚本 {} 失败: {}", scriptPath, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 从文件名中解析版本号。<p>
     * 取最后一个 {@code '.'} 前的最后一个分隔符（{@code _} 或 {@code -}）到 {@code '.'} 之间的数字。
     *
     * <pre>
     *   model_provider_init_1.sql  → 1
     *   settings_init-10.sql       → 10
     *   abc.sql                    → -1 (无分隔符)
     * </pre>
     *
     * @param filename 文件名，不含路径
     * @return 版本号，或 -1 表示无法解析
     */
    private int parseVersionFromFilename(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx <= 0) {
            return -1;
        }

        int lastUnderscore = filename.lastIndexOf('_', dotIdx);
        int lastHyphen = filename.lastIndexOf('-', dotIdx);
        int sepIdx = Math.max(lastUnderscore, lastHyphen);

        if (sepIdx < 0 || sepIdx >= dotIdx - 1) {
            return -1;
        }

        String versionStr = filename.substring(sepIdx + 1, dotIdx);
        try {
            return Integer.parseInt(versionStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 更新 {@code global_config} 中 {@code DB_INIT_VERSION} 的值。
     */
    private void updateVersion(int version) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE global_config SET config_val = ?, updated_at = ? WHERE config_key = ?",
                String.valueOf(version), now, VERSION_KEY);
    }
}
