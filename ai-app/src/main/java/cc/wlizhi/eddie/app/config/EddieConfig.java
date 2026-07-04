/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class EddieConfig {

    // ==================== 主库 ====================

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ==================== 通用 Bean ====================

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ==================== 虚拟线程配置 ====================

    /**
     * 配置 Tomcat 使用虚拟线程处理 HTTP 请求。
     * 需要配合 application.yml 中 {@code spring.threads.virtual.enabled: true} 使用。
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
        log.info("CPU核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("启用虚拟线程: Tomcat ProtocolHandler 配置为虚拟线程执行器");
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}
