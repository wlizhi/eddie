package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.McpServerEntity;
import jakarta.annotation.Resource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP 服务器配置表 (ai_mcp_server) 数据访问层
 */
@RegisterReflectionForBinding(McpServerEntity.class)
@Repository
public class McpServerDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 按名称查询 MCP 服务器
     */
    public Optional<McpServerEntity> findByName(String name) {
        String sql = """
                SELECT id, name, transport_type, command, args, env, url,
                       timeout_seconds, enabled, built_in, sort_order,
                       created_at, updated_at
                FROM ai_mcp_server
                WHERE name = ?
                """;
        List<McpServerEntity> results = jdbcTemplate.query(
                sql, new BeanPropertyRowMapper<>(McpServerEntity.class), name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 按 ID 查询 MCP 服务器
     */
    public Optional<McpServerEntity> findById(Long id) {
        String sql = """
                SELECT id, name, transport_type, command, args, env, url,
                       timeout_seconds, enabled, built_in, sort_order,
                       created_at, updated_at
                FROM ai_mcp_server
                WHERE id = ?
                """;
        List<McpServerEntity> results = jdbcTemplate.query(
                sql, new BeanPropertyRowMapper<>(McpServerEntity.class), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 查询所有已启用的 MCP 服务器
     */
    public List<McpServerEntity> findAllEnabled() {
        String sql = """
                SELECT id, name, transport_type, command, args, env, url,
                       timeout_seconds, enabled, built_in, sort_order,
                       created_at, updated_at
                FROM ai_mcp_server
                WHERE enabled = 1
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(McpServerEntity.class));
    }

    /**
     * 查询所有 MCP 服务器
     */
    public List<McpServerEntity> findAll() {
        String sql = """
                SELECT id, name, transport_type, command, args, env, url,
                       timeout_seconds, enabled, built_in, sort_order,
                       created_at, updated_at
                FROM ai_mcp_server
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(McpServerEntity.class));
    }

    /**
     * 插入 MCP 服务器
     */
    public void insert(McpServerEntity entity) {
        String sql = """
                INSERT INTO ai_mcp_server
                    (name, transport_type, command, args, env, url,
                     timeout_seconds, enabled, built_in, sort_order,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        datetime('now', 'localtime'), datetime('now', 'localtime'))
                """;
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getTransportType() != null ? entity.getTransportType() : "STREAMABLE_HTTP",
                entity.getCommand() != null ? entity.getCommand() : "",
                entity.getArgs() != null ? entity.getArgs() : "[]",
                entity.getEnv() != null ? entity.getEnv() : "{}",
                entity.getUrl() != null ? entity.getUrl() : "",
                entity.getTimeoutSeconds() != null ? entity.getTimeoutSeconds() : 60,
                entity.getEnabled() != null ? entity.getEnabled() : 1,
                entity.getBuiltIn() != null ? entity.getBuiltIn() : 0,
                entity.getSortOrder() != null ? entity.getSortOrder() : 0);
    }

    /**
     * 更新 MCP 服务器
     */
    public void update(McpServerEntity entity) {
        String sql = """
                UPDATE ai_mcp_server SET
                    name = ?, transport_type = ?, command = ?, args = ?, env = ?, url = ?,
                    timeout_seconds = ?, enabled = ?, built_in = ?, sort_order = ?,
                    updated_at = datetime('now', 'localtime')
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getTransportType(),
                entity.getCommand(),
                entity.getArgs(),
                entity.getEnv(),
                entity.getUrl(),
                entity.getTimeoutSeconds(),
                entity.getEnabled(),
                entity.getBuiltIn(),
                entity.getSortOrder(),
                entity.getId());
    }

    /**
     * 切换 MCP 服务器启用/禁用状态
     */
    public void updateEnabled(Long id, int enabled) {
        String sql = """
                UPDATE ai_mcp_server
                SET enabled = ?, updated_at = datetime('now', 'localtime')
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, enabled, id);
    }

    /**
     * 按 ID 删除 MCP 服务器
     */
    public void deleteById(Long id) {
        String sql = """
                DELETE FROM ai_mcp_server
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, id);
    }

    /**
     * 获取最后插入的自增 ID（SQLite）
     */
    public Long findLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }
}
