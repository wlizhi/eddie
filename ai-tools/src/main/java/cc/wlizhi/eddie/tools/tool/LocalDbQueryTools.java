/**
 * @author Eddie
 * {@code @date} 2026-07-11
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.common.tool.ToolBehavior.SecurityLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 内置本地数据库查询工具。<p>
 * 允许 AI 模型查询或写入本地 SQLite 数据库文件（eddie.db / eddie-agent.db）。<br>
 * 通过 {@code action} 参数区分行为：
 * <ul>
 *   <li><b>query</b> — 只读 SELECT 查询，默认自动放行（AUTO）</li>
 *   <li><b>execute</b> — INSERT/UPDATE/DELETE 写入，默认需用户审批（APPROVAL）</li>
 * </ul>
 * 可在工具设置页中覆盖每个行为的默认安全级别。
 */
@Component
public class LocalDbQueryTools implements BuiltInToolProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDbQueryTools.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询结果最大行数
     */
    private static final int MAX_ROWS = 500;

    /**
     * 查询超时（秒）
     */
    private static final int QUERY_TIMEOUT = 30;

    /**
     * SQL 语句最大长度，防止意外传入超长字符串
     */
    private static final int MAX_SQL_LENGTH = 10_000;

    @Override
    public String getMcpServerName() {
        return "BuiltInLocalDb";
    }

    @Override
    public List<ToolBehavior> getBehaviors() {
        return List.of(
                new ToolBehavior("query", "执行只读查询 SELECT",
                        "action", "query", SecurityLevel.AUTO),
                new ToolBehavior("write", "执行写入操作 INSERT/UPDATE/DELETE",
                        "action", "execute", SecurityLevel.APPROVAL)
        );
    }

    /**
     * 执行本地数据库查询或写入操作。
     *
     * <p><b>操作说明：</b>
     * <ul>
     *   <li>{@code action=query} — 执行 SELECT 查询，返回 JSON 格式的结果集</li>
     *   <li>{@code action=execute} — 执行 INSERT/UPDATE/DELETE，返回受影响行数</li>
     * </ul>
     *
     * <b>安全限制：</b>
     * <ul>
     *   <li>禁止执行 DDL（CREATE / DROP / ALTER / TRUNCATE / VACUUM 等）</li>
     *   <li>禁止多条语句同时执行</li>
     *   <li>查询结果最多返回 500 行</li>
     * </ul>
     *
     * @param action 操作类型：query（只读查询）/ execute（写入操作）
     * @param sql    要执行的 SQL 语句
     * @param dbName 数据库名称：eddie（默认）/ eddie-agent
     * @return 查询结果或受影响行数
     */
    @Tool(name = "built_in_db_query",
            description = """
                    操作本地 SQLite 数据库（eddie.db / eddie-agent.db）。
                    
                    **参数说明：**
                    - action：操作类型
                      • query — 只读查询（SELECT），返回 JSON 结果集
                      • execute — 写入操作（INSERT/UPDATE/DELETE），返回受影响行数
                    - sql：要执行的 SQL 语句
                    - dbName：数据库名称（可选），eddie（默认）/ eddie-agent
                    
                    **安全限制：**
                    - 禁止 DDL（CREATE/DROP/ALTER/TRUNCATE/VACUUM 等）
                    - 禁止多条语句同时执行
                    - SELECT 最多返回 500 行
                    - 写入操作需用户手动审批
                    """)
    public ApiResult<String> builtInDbQuery(
            @ToolParam(description = "操作类型：query（只读查询）/ execute（写入操作）") String action,
            @ToolParam(description = "要执行的 SQL 语句") String sql,
            @ToolParam(description = "数据库名称：eddie（默认）/ eddie-agent（可选）") String dbName) {

        // ===== 参数校验 =====
        if (action == null || action.isBlank()) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "action 不能为空");
        }
        if (sql == null || sql.isBlank()) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "sql 不能为空");
        }
        if (sql.length() > MAX_SQL_LENGTH) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "SQL 语句过长（超过 " + MAX_SQL_LENGTH + " 字符）");
        }
        if (dbName == null || dbName.isBlank()) {
            dbName = "eddie";
        }

        String normalizedAction = action.trim().toLowerCase();
        String normalizedSql = sql.trim();

        // ===== SQL 格式校验 =====
        // 禁止多条语句（分号分隔判断，忽略末尾分号）
        String sqlWithoutTrailingSemicolon = normalizedSql.endsWith(";")
                ? normalizedSql.substring(0, normalizedSql.length() - 1).trim()
                : normalizedSql;
        if (sqlWithoutTrailingSemicolon.contains(";")) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "禁止同时执行多条 SQL 语句");
        }

        // 提取第一个 token 判断 SQL 类型
        String sqlType = extractSqlType(normalizedSql);
        if (sqlType == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "无法识别的 SQL 语句");
        }

        // 禁止 DDL 操作
        if (isDdl(sqlType)) {
            return ApiResult.error(ApiResultCode.COMMAND_NOT_PERMITTED,
                    "禁止执行 DDL 操作（" + sqlType + "），仅允许数据查询和写入");
        }

        // 根据 action 校验 SQL 类型匹配
        switch (normalizedAction) {
            case "query":
                if (!isReadOperation(sqlType)) {
                    return ApiResult.error(ApiResultCode.BAD_REQUEST,
                            "action=query 仅支持 SELECT / PRAGMA / EXPLAIN，当前 SQL 类型: " + sqlType);
                }
                break;
            case "execute":
                if (!isWriteOperation(sqlType)) {
                    return ApiResult.error(ApiResultCode.BAD_REQUEST,
                            "action=execute 仅支持 INSERT / UPDATE / DELETE，当前 SQL 类型: " + sqlType);
                }
                break;
            default:
                return ApiResult.error(ApiResultCode.BAD_REQUEST, "不支持的 action 值，请使用 query 或 execute");
        }

        // ===== 解析数据库路径 =====
        String dataDir = System.getProperty("eddie.data");
        if (dataDir == null || dataDir.isBlank()) {
            log.warn("[LocalDbQueryTools] eddie.data 系统属性未设置，回退到默认路径");
            dataDir = System.getProperty("user.home") + "/.eddie/data";
        }

        String dbFile;
        switch (dbName.trim().toLowerCase()) {
            case "eddie-agent":
                dbFile = dataDir + "/eddie-agent.db";
                break;
            case "eddie":
            default:
                dbFile = dataDir + "/eddie.db";
                break;
        }

        log.info("[LocalDbQueryTools] action={}, db={}, sql={}", normalizedAction, dbName, normalizedSql);

        // ===== 执行 SQL =====
        Properties connProps = new Properties();
        connProps.setProperty("journal_mode", "WAL");
        connProps.setProperty("synchronous", "NORMAL");
        connProps.setProperty("cache_size", "10000");

        String jdbcUrl = "jdbc:sqlite:" + dbFile;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connProps)) {
            if ("query".equals(normalizedAction)) {
                return executeQuery(conn, normalizedSql);
            } else {
                return executeUpdate(conn, normalizedSql);
            }
        } catch (SQLException e) {
            log.warn("[LocalDbQueryTools] SQL 执行失败: action={}, sql={}", normalizedAction, normalizedSql, e);
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR,
                    "数据库操作失败: " + e.getMessage());
        }
    }

    /**
     * 执行 SELECT 查询，返回 JSON 格式结果集。
     */
    private ApiResult<String> executeQuery(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT);
            stmt.setMaxRows(MAX_ROWS);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                List<Map<String, Object>> rows = new ArrayList<>();
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    rows.add(row);
                }

                StringBuilder result = new StringBuilder();
                result.append("查询完成（共 ").append(rowCount).append(" 行）\n\n");
                try {
                    String json = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(rows);
                    result.append(json);
                } catch (Exception e) {
                    log.warn("[LocalDbQueryTools] JSON 序列化失败，回退到文本格式", e);
                    result.append("结果集 ").append(rowCount).append(" 行 × ").append(columnCount).append(" 列");
                    for (Map<String, Object> row : rows) {
                        result.append("\n").append(row);
                    }
                }

                if (rowCount >= MAX_ROWS) {
                    result.append("\n\n⚠️ 结果已截断，仅显示前 ").append(MAX_ROWS).append(" 行");
                }

                return ApiResult.success(result.toString());
            }
        } catch (SQLException e) {
            log.warn("[LocalDbQueryTools] 查询执行失败: {}", sql, e);
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR,
                    "查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行 INSERT/UPDATE/DELETE，返回受影响行数或插入行的自增 ID。
     * <ul>
     *   <li>INSERT — 返回新插入行的自增 ID（rowid）</li>
     *   <li>UPDATE/DELETE — 返回受影响行数</li>
     * </ul>
     */
    private ApiResult<String> executeUpdate(Connection conn, String sql) {
        String sqlType = extractSqlType(sql);
        boolean isInsert = "INSERT".equals(sqlType);

        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT);

            if (isInsert) {
                stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys != null && keys.next()) {
                        long rowId = keys.getLong(1);
                        String msg = "插入成功，自增 ID: " + rowId;
                        log.info("[LocalDbQueryTools] {}", msg);
                        return ApiResult.success(msg);
                    }
                    // 无生成键的表（如无 rowid 的虚拟表），回退到 last_insert_rowid()
                    try (Statement idStmt = conn.createStatement();
                         ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            long rowId = rs.getLong(1);
                            String msg = "插入成功，自增 ID: " + rowId;
                            log.info("[LocalDbQueryTools] {}", msg);
                            return ApiResult.success(msg);
                        }
                    }
                    String msg = "插入成功，但无法获取自增 ID";
                    log.warn("[LocalDbQueryTools] {}", msg);
                    return ApiResult.success(msg);
                }
            } else {
                int affectedRows = stmt.executeUpdate(sql);
                String msg = "操作成功，受影响行数: " + affectedRows;
                log.info("[LocalDbQueryTools] {}", msg);
                return ApiResult.success(msg);
            }
        } catch (SQLException e) {
            log.warn("[LocalDbQueryTools] 写入操作执行失败: {}", sql, e);
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR,
                    "写入操作失败: " + e.getMessage());
        }
    }

    /**
     * 从 SQL 语句中提取第一个 token（关键字）。
     */
    private static String extractSqlType(String sql) {
        // 去除开头的空白和注释
        String cleaned = sql.stripLeading();
        if (cleaned.startsWith("--")) {
            int nl = cleaned.indexOf('\n');
            if (nl < 0) return null;
            cleaned = cleaned.substring(nl + 1).stripLeading();
        }
        int firstSpace = cleaned.indexOf(' ');
        int firstParen = cleaned.indexOf('(');
        int endIdx;
        if (firstSpace > 0 && firstParen > 0) {
            endIdx = Math.min(firstSpace, firstParen);
        } else if (firstSpace > 0) {
            endIdx = firstSpace;
        } else if (firstParen > 0) {
            endIdx = firstParen;
        } else {
            // 无空格也无括号，可能是单 token 语句
            if (cleaned.endsWith(";")) {
                return cleaned.substring(0, cleaned.length() - 1).toUpperCase();
            }
            // 检查仅由字母组成
            if (cleaned.matches("(?i)[a-z]+")) {
                return cleaned.toUpperCase();
            }
            return null;
        }
        return cleaned.substring(0, endIdx).toUpperCase();
    }

    /**
     * 判断 SQL 类型是否为 DDL（禁止执行）。
     */
    private static boolean isDdl(String sqlType) {
        return switch (sqlType) {
            case "CREATE", "DROP", "ALTER", "TRUNCATE", "VACUUM",
                 "REINDEX", "ANALYZE", "ATTACH", "DETACH" -> true;
            default -> false;
        };
    }

    /**
     * 判断 SQL 类型是否为只读操作。
     */
    private static boolean isReadOperation(String sqlType) {
        return "SELECT".equals(sqlType) || "PRAGMA".equals(sqlType) || "EXPLAIN".equals(sqlType);
    }

    /**
     * 判断 SQL 类型是否为写入操作。
     */
    private static boolean isWriteOperation(String sqlType) {
        return "INSERT".equals(sqlType) || "UPDATE".equals(sqlType) || "DELETE".equals(sqlType)
                || "REPLACE".equals(sqlType) || "UPSERT".equals(sqlType);
    }
}
