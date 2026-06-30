/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.dao;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天模型选择器数据访问层
 * 直连 model_provider 表，查询启用的供应商及其模型列表
 */
@Repository
public class ChatModelProviderDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询所有启用的供应商及其 models
     */
    public List<EnabledProviderModel> findAllEnabledWithModels() {
        String sql = """
                SELECT id, code, name, models
                FROM model_provider
                WHERE enabled = 1
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(EnabledProviderModel.class));
    }

    /**
     * 查询结果行映射（内部类，不对外暴露）
     */
    @Getter
    @Setter
    public static class EnabledProviderModel {
        private Long id;
        private String code;
        private String name;
        private String models;
    }
}
