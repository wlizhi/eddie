/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Owner 工具绑定表 (ai_owner_tool_binding) 数据访问层
 * <p>
 * 多态关联 {@code ai_tool_definition}，支持 BUILT_IN 和 MCP 两种类型。
 * 绑定记录不关心工具是否启用，运行时按当前全局启用状态动态过滤。
 */
@Repository
public class OwnerToolBindingDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询指定 Owner 绑定的工具定义列表（不过滤 td.enabled）
     * <p>
     * 运行时再根据 {@code td.enabled = 1} 和 {@code ms.enabled = 1} 动态过滤。
     */
    public List<ToolDefinitionEntity> findBoundTools(RoleType ownerType, Long ownerId) {
        String sql = """
                SELECT td.id, td.tool_type, td.name, td.display_name, td.description,
                       td.enabled, td.built_in, td.mcp_server_id, td.sort_order,
                       td.created_at, td.updated_at
                FROM ai_owner_tool_binding b
                JOIN ai_tool_definition td ON b.tool_id = td.id
                WHERE b.owner_type = ? AND b.owner_id = ? AND b.enabled = 1
                ORDER BY td.sort_order ASC, td.id ASC
                """;
        return jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(ToolDefinitionEntity.class),
                ownerType.name(), ownerId);
    }

    /**
     * 查询指定 Owner 已绑定的 MCP Server ID 列表（去重）
     */
    public List<Long> findBoundMcpServerIds(RoleType ownerType, Long ownerId) {
        String sql = """
                SELECT DISTINCT td.mcp_server_id
                FROM ai_owner_tool_binding b
                JOIN ai_tool_definition td ON b.tool_id = td.id
                WHERE b.owner_type = ? AND b.owner_id = ? AND b.enabled = 1
                      AND td.mcp_server_id IS NOT NULL
                ORDER BY td.mcp_server_id ASC
                """;
        return jdbcTemplate.queryForList(sql, Long.class, ownerType.name(), ownerId);
    }

    /**
     * 查询所有 Owner 的绑定关系（用于全量缓存）
     * <p>
     * 返回格式：owner_id, owner_type, tool_id, enabled
     */
    public List<OwnerToolBindingRow> findAllBindings() {
        String sql = """
                SELECT id, owner_type, owner_id, tool_id, enabled, created_at
                FROM ai_owner_tool_binding
                WHERE enabled in (1,2)
                ORDER BY owner_type, owner_id, id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OwnerToolBindingRow row = new OwnerToolBindingRow();
            row.setId(rs.getLong("id"));
            row.setOwnerType(rs.getString("owner_type"));
            row.setOwnerId(rs.getLong("owner_id"));
            row.setToolId(rs.getLong("tool_id"));
            row.setEnabled(rs.getInt("enabled"));
            return row;
        });
    }

    /**
     * 删除指定 Owner 的全部绑定
     */
    public void deleteByOwner(RoleType ownerType, Long ownerId) {
        jdbcTemplate.update(
                "DELETE FROM ai_owner_tool_binding WHERE owner_type = ? AND owner_id = ?",
                ownerType.name(), ownerId);
    }

    /**
     * 批量插入绑定关系（使用 JDBC batchUpdate，单次网络往返）
     */
    public void batchInsert(RoleType ownerType, Long ownerId, List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) return;
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO ai_owner_tool_binding (owner_type, owner_id, tool_id, enabled, created_at)
                VALUES (?, ?, ?, 1, ?)
                """;
        List<Object[]> batchArgs = toolIds.stream()
                .map(toolId -> new Object[]{ownerType.name(), ownerId, toolId, now})
                .toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 删除指定 Owner 下属于指定工具 ID 列表的所有绑定
     */
    public void deleteByOwnerAndToolIds(RoleType ownerType, Long ownerId, List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) return;
        String placeholders = String.join(",", toolIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = String.format("""
                DELETE FROM ai_owner_tool_binding
                WHERE owner_type = ? AND owner_id = ?
                AND tool_id IN (%s)
                """, placeholders);
        Object[] params = new Object[toolIds.size() + 2];
        params[0] = ownerType.name();
        params[1] = ownerId;
        for (int i = 0; i < toolIds.size(); i++) {
            params[i + 2] = toolIds.get(i);
        }
        jdbcTemplate.update(sql, params);
    }

    /**
     * 批量插入绑定关系（每行带 enabled 状态）
     * <p>
     * 与 batchInsert 不同，此方法允许指定每个绑定的 enabled 值（0/1/2）。
     *
     * @param ownerType 归属方类型
     * @param ownerId   归属方 ID
     * @param bindings  绑定行列表（含 toolId + enabled）
     */
    public void batchInsertWithStatus(RoleType ownerType, Long ownerId, List<OwnerToolBindingRow> bindings) {
        if (bindings == null || bindings.isEmpty()) return;
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO ai_owner_tool_binding (owner_type, owner_id, tool_id, enabled, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        List<Object[]> batchArgs = bindings.stream()
                .map(b -> new Object[]{ownerType.name(), ownerId, b.getToolId(), b.getEnabled(), now})
                .toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 查询指定 Owner 的全部绑定（含 enabled=0）
     * <p>
     * 与 findAllBindings 不同，此方法不过滤 enabled，返回所有记录。
     */
    public List<OwnerToolBindingRow> findAllBindingsByOwner(String ownerType, Long ownerId) {
        String sql = """
                SELECT id, owner_type, owner_id, tool_id, enabled, created_at
                FROM ai_owner_tool_binding
                WHERE owner_type = ? AND owner_id = ?
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OwnerToolBindingRow row = new OwnerToolBindingRow();
            row.setId(rs.getLong("id"));
            row.setOwnerType(rs.getString("owner_type"));
            row.setOwnerId(rs.getLong("owner_id"));
            row.setToolId(rs.getLong("tool_id"));
            row.setEnabled(rs.getInt("enabled"));
            return row;
        }, ownerType, ownerId);
    }

    /**
     * 按工具 ID 列表删除所有绑定记录（不区分 Owner）
     */
    public void deleteByToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) return;
        String placeholders = String.join(",", toolIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = String.format("""
                DELETE FROM ai_owner_tool_binding
                WHERE tool_id IN (%s)
                """, placeholders);
        jdbcTemplate.update(sql, toolIds.toArray());
    }

    /**
     * 绑定行映射（内部使用）
     */
    @Getter
    @Setter
    public static class OwnerToolBindingRow {
        private Long id;
        private String ownerType;
        private Long ownerId;
        private Long toolId;
        private Integer enabled;
    }
}
