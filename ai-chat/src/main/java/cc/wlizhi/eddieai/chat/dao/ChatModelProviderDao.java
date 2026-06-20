package cc.wlizhi.eddieai.chat.dao;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天模型选择器数据访问层
 * 直连 model_provider 表，查询启用的供应商及其模型列表
 */
@RegisterReflectionForBinding(ChatModelProviderDao.EnabledProviderModel.class)
@Repository
public class ChatModelProviderDao {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询所有启用的供应商及其 models（仅返回分组展示需要的字段）
     */
    public List<EnabledProviderModel> findAllEnabledWithModels() {
        String sql = """
                SELECT code, name, models
                FROM model_provider
                WHERE enabled = 1
                ORDER BY sort_order ASC, code ASC
                """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(EnabledProviderModel.class));
    }

    /**
     * 查询结果行映射（内部类，不对外暴露）
     */
    @Getter
    @Setter
    public static class EnabledProviderModel {
        private String code;
        private String name;
        private String models;
    }
}
