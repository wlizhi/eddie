/**
 * @author Eddie
 * {@code @date} 2026-07-11
 */

package cc.wlizhi.eddie.common.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * JDBC 工具类 — 提供 INSERT 返回自增主键等通用方法
 */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    /**
     * 执行 INSERT 并返回自增主键
     * <p>
     * 基于 {@link Statement#RETURN_GENERATED_KEYS} 机制，与连接池大小无关，线程安全。
     * 兼容 SQLite（xerial sqlite-jdbc 原生支持）。
     *
     * @param jdbcTemplate JdbcTemplate
     * @param sql          INSERT SQL
     * @param params       SQL 参数（按顺序）
     * @return 自增主键 ID
     */
    public static Long insertAndReturnKey(JdbcTemplate jdbcTemplate, String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }
}
