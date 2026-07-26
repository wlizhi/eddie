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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 版本化数据库迁移/初始化脚本执行器。<p>
 * 支持主库（eddie.db）和 Agent 库（eddie-agent.db），双库各自维护版本号。
 * 两个库统一使用 {@code global_config} 表中的 {@code DB_INIT_VERSION} 键追踪版本。
 *
 * <h3>文件名规则</h3>
 * <pre>
 *   model_provider_init_1.sql  → 版本 1
 *   settings_init-10.sql       → 版本 10
 *   agent-v1.sql               → 版本 1
 * </pre>
 * 解析规则：取最后一个 {@code _} 或 {@code -} 到扩展名 {@code .} 之间的数字作为版本号。
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>查询各自 {@code global_config} 中 {@code DB_INIT_VERSION} 的值</li>
 *   <li>无记录 → 插入版本 {@code 0}</li>
 *   <li>按配置顺序扫描所有 {@code .sql} 文件，解析版本号</li>
 *   <li>筛选版本号 > 当前版本的脚本，按版本号升序执行</li>
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
        // 确保 Agent 库有 global_config 表（与主库一致的版本追踪机制）
        ensureGlobalConfigTable(agentJdbcTemplate);

        // 注册两个任务：主库先执行（10），Agent 库后执行（15）
        initScheduler.addTask("mainDbInit", 10, () ->
                executePendingMigrations(jdbcTemplate,
                        eddieProperties.getMigrationScripts(),
                        eddieProperties.getInitScripts()), true);
        initScheduler.addTask("agentDbInit", 15, () ->
                executePendingMigrations(agentJdbcTemplate,
                        eddieProperties.getAgentMigrationScripts(),
                        eddieProperties.getAgentInitScripts()), true);
    }

    /**
     * 确保目标数据库存在 global_config 表（用于版本追踪）。
     */
    private void ensureGlobalConfigTable(JdbcTemplate jt) {
        jt.execute("""
                CREATE TABLE IF NOT EXISTS global_config (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    config_key  TEXT NOT NULL UNIQUE,
                    config_val  TEXT NOT NULL DEFAULT '{}',
                    config_type TEXT NOT NULL DEFAULT 'FRONTEND',
                    description TEXT NOT NULL DEFAULT '',
                    updated_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                )
                """);
    }

    /**
     * 对指定数据库执行待处理的迁移脚本和初始化脚本。<p>
     * 先执行迁移脚本（DDL），再执行初始化脚本（数据），
     * 统一使用当前库的 {@code DB_INIT_VERSION} 追踪版本。
     */
    private void executePendingMigrations(JdbcTemplate jt, List<String> migrationScripts, List<String> initScripts) {
        int currentVersion = getCurrentVersion(jt);
        log.info("当前数据库初始化版本: {}", currentVersion);

        List<VersionedScript> allScripts = scanVersionedSqlFiles(migrationScripts, initScripts, currentVersion);
        if (allScripts.isEmpty()) {
            log.info("没有待执行的数据库初始化脚本");
            return;
        }

        List<VersionedScript> toExecute = allScripts.stream()
                .filter(s -> s.version() > currentVersion)
                .toList();

        if (toExecute.isEmpty()) {
            log.info("数据库已是最新版本 (v{})", currentVersion);
            return;
        }

        log.info("待执行的脚本: {}", toExecute.stream()
                .map(s -> "v" + s.version() + " (" + s.scriptPath() + ")")
                .toList());

        int maxVersion = currentVersion;
        for (VersionedScript script : toExecute) {
            int version = script.version();
            String path = script.scriptPath();
            try {
                log.info("执行脚本 [{}] v{}...", path, version);
                executeSqlScript(jt, script.sql());
                maxVersion = Math.max(maxVersion, version);
                log.info("脚本 [{}] v{} 执行成功", path, version);
            } catch (Exception e) {
                log.error("脚本 [{}] v{} 执行失败: {}", path, version, e.getMessage());
            }
        }

        if (maxVersion > currentVersion) {
            updateVersion(jt, maxVersion);
            log.info("数据库初始化版本已更新至 v{}", maxVersion);
        }
    }

    /**
     * 执行 SQL 脚本内容。<p>
     * 先移除所有 {@code --} 注释行，再按 {@code ;} 拆分逐条执行。
     */
    private void executeSqlScript(JdbcTemplate jt, String script) {
        String cleaned = script.replaceAll("(?m)^--.*$", "");
        String[] statements = cleaned.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            jt.execute(trimmed);
        }
    }

    /**
     * 获取当前已执行的数据库初始化版本号。
     */
    private int getCurrentVersion(JdbcTemplate jt) {
        String sql = "SELECT config_val FROM global_config WHERE config_key = ?";
        var results = jt.query(sql, (rs, rowNum) -> rs.getString("config_val"), VERSION_KEY);
        if (results.isEmpty()) {
            jt.update(
                    "INSERT INTO global_config (config_key, config_val, description) VALUES (?, '0', '数据库初始化版本号')",
                    VERSION_KEY);
            return 0;
        }
        try {
            return Integer.parseInt(results.get(0));
        } catch (NumberFormatException e) {
            log.warn("全局配置 DB_INIT_VERSION 值异常: {}, 重置为 0", results.get(0));
            jt.update("UPDATE global_config SET config_val = '0' WHERE config_key = ?", VERSION_KEY);
            return 0;
        }
    }

    /**
     * 扫描迁移脚本和初始化脚本，读取文件内容返回，已执行的跳过。
     */
    private List<VersionedScript> scanVersionedSqlFiles(List<String> migrationScripts, List<String> initScripts, int currentVersion) {
        List<VersionedScript> result = new ArrayList<>();
        // 先扫迁移脚本（DDL），再扫初始化脚本（数据），确保 DDL 先执行
        for (String scriptPath : migrationScripts) {
            addVersionedScript(result, scriptPath, currentVersion);
        }
        for (String scriptPath : initScripts) {
            addVersionedScript(result, scriptPath, currentVersion);
        }
        result.sort(Comparator.comparingInt(VersionedScript::version));
        return result;
    }

    /**
     * 将版本化 SQL 脚本添加到结果列表（已执行的跳过）。
     */
    private void addVersionedScript(List<VersionedScript> result, String scriptPath, int currentVersion) {
        String filename = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);
        int version = parseVersionFromFilename(filename);
        if (version < 0) {
            log.debug("跳过不匹配的 SQL 文件: {}", filename);
            return;
        }
        if (version <= currentVersion) {
            log.debug("跳过已执行的脚本 [{}] v{}", scriptPath, version);
            return;
        }
        org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + scriptPath);
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String sql = reader.lines().collect(Collectors.joining("\n")).trim();
            if (sql.isEmpty()) {
                log.warn("脚本 [{}] v{} 内容为空，跳过", scriptPath, version);
                return;
            }
            result.add(new VersionedScript(version, scriptPath, sql));
        } catch (Exception e) {
            log.error("加载脚本 {} 失败: {}", scriptPath, e.getMessage());
        }
    }

    /**
     * 脚本文件及其版本号的内部记录。
     */
    private record VersionedScript(int version, String scriptPath, String sql) {
    }

    /**
     * 从文件名中解析版本号。<p>
     * 取最后一个 {@code '.'} 前的最后一个分隔符（{@code _} 或 {@code -}）到 {@code '.'} 之间的数字。
     *
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
     * 更新 {@code global_config} 中 {@code DB_INIT_VERSION} 的值。
     */
    private void updateVersion(JdbcTemplate jt, int version) {
        long now = System.currentTimeMillis();
        jt.update(
                "UPDATE global_config SET config_val = ?, updated_at = ? WHERE config_key = ?",
                String.valueOf(version), now, VERSION_KEY);
    }
}
