package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.common.config.EddieProperties;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库迁移/初始化脚本执行器。<p>
 * 在一个任务中按顺序执行主库（{@code eddie.db}）和 Agent 库（{@code eddie-agent.db}）的 DDL/数据脚本，
 * 版本号统一存储在 {@code eddie.db} 的 {@code global_config} 表（键名 {@code DB_INIT_VERSION}）。
 *
 * <h3>执行顺序</h3>
 * <ol>
 *   <li>主库 DDL 迁移脚本</li>
 *   <li>Agent 库 DDL 迁移脚本</li>
 *   <li>主库数据初始化脚本</li>
 *   <li>Agent 库数据初始化脚本</li>
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
    @Qualifier("agentJdbcTemplate")
    private JdbcTemplate agentJdbcTemplate;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private EddieProperties eddieProperties;

    @Resource
    private InitScheduler initScheduler;

    @PostConstruct
    public void init() {
        initScheduler.addTask("dbInit", 10, this::executePendingMigrations, true);
    }

    /**
     * 统一执行所有待处理的迁移/初始化脚本。
     */
    private void executePendingMigrations() {
        int currentVersion = getCurrentVersion();
        log.info("当前数据库初始化版本: {}", currentVersion);

        // 按顺序收集所有待执行的脚本任务
        List<ScriptTask> tasks = new ArrayList<>();
        collectTasks(tasks, jdbcTemplate, eddieProperties.getMigrationScripts(), currentVersion);
        collectTasks(tasks, agentJdbcTemplate, eddieProperties.getAgentMigrationScripts(), currentVersion);
        collectTasks(tasks, jdbcTemplate, eddieProperties.getInitScripts(), currentVersion);
        collectTasks(tasks, agentJdbcTemplate, eddieProperties.getAgentInitScripts(), currentVersion);

        if (tasks.isEmpty()) {
            log.info("没有待执行的数据库初始化脚本");
            return;
        }

        int maxVersion = currentVersion;
        for (ScriptTask task : tasks) {
            maxVersion = Math.max(maxVersion, task.version);
            executeScript(task);
        }

        if (maxVersion > currentVersion) {
            updateVersion(maxVersion);
            log.info("数据库初始化版本已更新至 v{}", maxVersion);
        }
    }

    /**
     * 从脚本路径列表中扫描版本号 > {@code currentVersion} 的脚本，并以任务形式添加。
     */
    private void collectTasks(List<ScriptTask> tasks, JdbcTemplate jt, List<String> scriptPaths, int currentVersion) {
        for (String scriptPath : scriptPaths) {
            String filename = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);
            int version = parseVersionFromFilename(filename);
            if (version < 0) {
                log.debug("跳过不匹配的 SQL 文件: {}", filename);
                continue;
            }
            if (version <= currentVersion) {
                log.debug("跳过已执行的脚本 [{}] v{}", scriptPath, version);
                continue;
            }
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + scriptPath);
            try (var reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sql = reader.lines().collect(Collectors.joining("\n")).trim();
                if (sql.isEmpty()) {
                    log.warn("脚本 [{}] v{} 内容为空，跳过", scriptPath, version);
                    continue;
                }
                tasks.add(new ScriptTask(jt, sql, version, scriptPath));
            } catch (Exception e) {
                log.error("加载脚本 {} 失败: {}", scriptPath, e.getMessage());
            }
        }
    }

    /**
     * 执行单个脚本。
     */
    private void executeScript(ScriptTask task) {
        try {
            log.info("执行脚本 [{}] v{}...", task.path, task.version);
            String cleaned = task.sql.replaceAll("(?m)^--.*$", "");
            for (String stmt : cleaned.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    task.jt.execute(trimmed);
                }
            }
            log.info("脚本 [{}] v{} 执行成功", task.path, task.version);
        } catch (Exception e) {
            log.error("脚本 [{}] v{} 执行失败: {}", task.path, task.version, e.getMessage());
        }
    }

    /**
     * 从主库 {@code global_config} 获取当前已执行的版本号。
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

    /**
     * 更新主库 {@code global_config} 中的版本号。
     */
    private void updateVersion(int version) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "UPDATE global_config SET config_val = ?, updated_at = ? WHERE config_key = ?",
                String.valueOf(version), now, VERSION_KEY);
    }

    /**
     * 从文件名中解析版本号。
     * <pre>
     *   model_provider_init_1.sql  → 1
     *   settings_init-10.sql       → 10
     *   agent-v1.sql               → 1
     *   abc.sql                    → -1 (无分隔符)
     * </pre>
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
     * 脚本执行任务：包含目标数据源、SQL 内容、版本号、路径。
     */
    private static class ScriptTask {
        final JdbcTemplate jt;
        final String sql;
        final int version;
        final String path;

        ScriptTask(JdbcTemplate jt, String sql, int version, String path) {
            this.jt = jt;
            this.sql = sql;
            this.version = version;
            this.path = path;
        }
    }
}
