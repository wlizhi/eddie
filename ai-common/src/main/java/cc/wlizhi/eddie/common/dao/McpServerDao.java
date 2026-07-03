/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.McpServerEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP 服务器配置表 (ai_mcp_server) 数据访问层
 */
@Repository
public class McpServerDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 全字段查询列（不含关联表）
     */
    private static final String ALL_COLUMNS = """
            id, name, description, source_type, source_config, transport_type,
            command, args, env, url, headers,
            timeout_seconds, enabled, sort_order,
            reconnect_interval_sec, max_reconnect_attempts,
            created_at, updated_at
            """;

    /**
     * 按名称查询 MCP 服务器
     */
    public Optional<McpServerEntity> findByName(String name) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM ai_mcp_server WHERE name = ?";
        List<McpServerEntity> results = jdbcTemplate.query(
                sql, new BeanPropertyRowMapper<>(McpServerEntity.class), name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 按 ID 查询 MCP 服务器
     */
    public Optional<McpServerEntity> findById(Long id) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM ai_mcp_server WHERE id = ?";
        List<McpServerEntity> results = jdbcTemplate.query(
                sql, new BeanPropertyRowMapper<>(McpServerEntity.class), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 查询所有已启用的 MCP 服务器
     */
    public List<McpServerEntity> findAllEnabled() {
        String sql = "SELECT " + ALL_COLUMNS
                + " FROM ai_mcp_server WHERE enabled = 1 ORDER BY sort_order ASC, id ASC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(McpServerEntity.class));
    }

    /**
     * 查询所有 MCP 服务器
     */
    public List<McpServerEntity> findAll() {
        String sql = "SELECT " + ALL_COLUMNS
                + " FROM ai_mcp_server ORDER BY sort_order ASC, id ASC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(McpServerEntity.class));
    }

    /**
     * 插入 MCP 服务器
     */
    public void insert(McpServerEntity entity) {
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO ai_mcp_server
                    (name, description, source_type, source_config, transport_type,
                     command, args, env, url, headers,
                     timeout_seconds, enabled, sort_order,
                     reconnect_interval_sec, max_reconnect_attempts,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?,
                        ?, ?,
                        ?, ?)
                """;
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getDescription() != null ? entity.getDescription() : "",
                entity.getSourceType() != null ? entity.getSourceType() : "USER",
                entity.getSourceConfig() != null ? entity.getSourceConfig() : "{}",
                entity.getTransportType() != null ? entity.getTransportType() : "STREAMABLE_HTTP",
                entity.getCommand() != null ? entity.getCommand() : "",
                entity.getArgs() != null ? entity.getArgs() : "[]",
                entity.getEnv() != null ? entity.getEnv() : "",
                entity.getUrl() != null ? entity.getUrl() : "",
                entity.getHeaders() != null ? entity.getHeaders() : "",
                entity.getTimeoutSeconds() != null ? entity.getTimeoutSeconds() : 60,
                entity.getEnabled() != null ? entity.getEnabled() : 1,
                entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                entity.getReconnectIntervalSec(),
                entity.getMaxReconnectAttempts(),
                now, now);
    }

    /**
     * 更新 MCP 服务器
     */
    public void update(McpServerEntity entity) {
        long now = System.currentTimeMillis();
        String sql = """
                UPDATE ai_mcp_server SET
                    name = ?, description = ?, source_type = ?, source_config = ?,
                    transport_type = ?, command = ?, args = ?,
                    env = ?, url = ?, headers = ?,
                    timeout_seconds = ?, enabled = ?, sort_order = ?,
                    reconnect_interval_sec = ?, max_reconnect_attempts = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getDescription(),
                entity.getSourceType(),
                entity.getSourceConfig(),
                entity.getTransportType(),
                entity.getCommand(),
                entity.getArgs(),
                entity.getEnv(),
                entity.getUrl(),
                entity.getHeaders(),
                entity.getTimeoutSeconds(),
                entity.getEnabled(),
                entity.getSortOrder(),
                entity.getReconnectIntervalSec(),
                entity.getMaxReconnectAttempts(),
                now,
                entity.getId());
    }

    /**
     * 切换 MCP 服务器启用/禁用状态
     */
    public void updateEnabled(Long id, int enabled) {
        long now = System.currentTimeMillis();
        String sql = """
                UPDATE ai_mcp_server
                SET enabled = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, enabled, now, id);
    }

    /**
     * 按 ID 删除 MCP 服务器
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM ai_mcp_server WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 获取最后插入的自增 ID（SQLite）
     */
    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }
}
