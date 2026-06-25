package cc.wlizhi.eddie.common.dao;

import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.RoleType;
import jakarta.annotation.Resource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
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
@RegisterReflectionForBinding(ToolDefinitionEntity.class)
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
                WHERE enabled = 1
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
     * 批量插入绑定关系
     */
    public void batchInsert(RoleType ownerType, Long ownerId, List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) return;
        String sql = """
                INSERT INTO ai_owner_tool_binding (owner_type, owner_id, tool_id, enabled, created_at)
                VALUES (?, ?, ?, 1, datetime('now', 'localtime'))
                """;
        for (Long toolId : toolIds) {
            jdbcTemplate.update(sql, ownerType.name(), ownerId, toolId);
        }
    }

    /**
     * 绑定行映射（内部使用）
     */
    public static class OwnerToolBindingRow {
        private Long id;
        private String ownerType;
        private Long ownerId;
        private Long toolId;
        private Integer enabled;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOwnerType() {
            return ownerType;
        }

        public void setOwnerType(String ownerType) {
            this.ownerType = ownerType;
        }

        public Long getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(Long ownerId) {
            this.ownerId = ownerId;
        }

        public Long getToolId() {
            return toolId;
        }

        public void setToolId(Long toolId) {
            this.toolId = toolId;
        }

        public Integer getEnabled() {
            return enabled;
        }

        public void setEnabled(Integer enabled) {
            this.enabled = enabled;
        }
    }
}
