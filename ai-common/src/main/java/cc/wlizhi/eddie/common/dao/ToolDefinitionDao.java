/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具定义表 (ai_tool_definition) 数据访问层
 */
@Repository
public class ToolDefinitionDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 按 ID 查询工具定义
     */
    public ToolDefinitionEntity findById(Long id) {
        String sql = """
                SELECT id, tool_type, name, display_name, description,
                       enabled, built_in, mcp_server_id, sort_order,
                       created_at, updated_at
                FROM ai_tool_definition
                WHERE id = ?
                """;
        List<ToolDefinitionEntity> results = jdbcTemplate.query(
                sql, new BeanPropertyRowMapper<>(ToolDefinitionEntity.class), id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询所有内置工具
     */
    public List<ToolDefinitionEntity> findAllBuiltIn() {
        String sql = """
                SELECT id, tool_type, name, display_name, description,
                       enabled, built_in, mcp_server_id, sort_order,
                       created_at, updated_at
                FROM ai_tool_definition
                WHERE built_in = 1
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ToolDefinitionEntity.class));
    }

    /**
     * 插入工具定义
     */
    public void insert(ToolDefinitionEntity entity) {
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO ai_tool_definition
                    (tool_type, name, display_name, description, enabled,
                     built_in, mcp_server_id, sort_order,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?)
                """;
        jdbcTemplate.update(sql,
                entity.getToolType(),
                entity.getName(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getEnabled() != null ? entity.getEnabled() : 1,
                entity.getBuiltIn() != null ? entity.getBuiltIn() : 0,
                entity.getMcpServerId(),
                entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                now, now);
    }

    /**
     * 更新工具定义（排除创建时间）
     */
    public void update(ToolDefinitionEntity entity) {
        long now = System.currentTimeMillis();
        String sql = """
                UPDATE ai_tool_definition SET
                    tool_type = ?, display_name = ?, description = ?,
                    enabled = ?, built_in = ?, mcp_server_id = ?,
                    sort_order = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                entity.getToolType(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getBuiltIn(),
                entity.getMcpServerId(),
                entity.getSortOrder(),
                now,
                entity.getId());
    }

    /**
     * 查询所有工具定义（不过滤 enabled 状态）
     */
    public List<ToolDefinitionEntity> findAll() {
        String sql = """
                SELECT id, tool_type, name, display_name, description,
                       enabled, built_in, mcp_server_id, sort_order,
                       created_at, updated_at
                FROM ai_tool_definition
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ToolDefinitionEntity.class));
    }

    /**
     * 切换启用/禁用/待审批状态
     * <p>
     * enabled: 0=禁用, 1=启用, 2=待审批
     */
    public void updateEnabled(Long id, int enabled) {
        long now = System.currentTimeMillis();
        String sql = "UPDATE ai_tool_definition SET enabled = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, enabled, now, id);
    }

    /**
     * 按 MCP Server ID 查询工具列表（不过滤启用状态）
     */
    public List<ToolDefinitionEntity> findByMcpServerId(Long mcpServerId) {
        String sql = """
                SELECT id, tool_type, name, display_name, description,
                       enabled, built_in, mcp_server_id, sort_order,
                       created_at, updated_at
                FROM ai_tool_definition
                WHERE mcp_server_id = ?
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ToolDefinitionEntity.class), mcpServerId);
    }

    /**
     * 按 MCP Server ID 删除所有工具定义
     */
    public void deleteByMcpServerId(Long mcpServerId) {
        String sql = """
                DELETE FROM ai_tool_definition
                WHERE mcp_server_id = ?
                """;
        jdbcTemplate.update(sql, mcpServerId);
    }

    /**
     * 按 ID 删除工具定义
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM ai_tool_definition WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 批量插入工具定义
     */
    public void batchInsert(List<ToolDefinitionEntity> entities) {
        if (entities == null || entities.isEmpty()) return;
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO ai_tool_definition
                    (tool_type, name, display_name, description, enabled,
                     built_in, mcp_server_id, sort_order,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?)
                """;
        List<Object[]> batchArgs = new ArrayList<>(entities.size());
        for (ToolDefinitionEntity entity : entities) {
            batchArgs.add(new Object[]{
                    entity.getToolType() != null ? entity.getToolType().name() : "MCP",
                    entity.getName(),
                    entity.getDisplayName() != null ? entity.getDisplayName() : "",
                    entity.getDescription() != null ? entity.getDescription() : "",
                    entity.getEnabled() != null ? entity.getEnabled() : 1,
                    entity.getBuiltIn() != null ? entity.getBuiltIn() : 0,
                    entity.getMcpServerId(),
                    entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                    now, now
            });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
