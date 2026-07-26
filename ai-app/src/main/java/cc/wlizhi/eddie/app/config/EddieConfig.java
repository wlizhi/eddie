/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.config.EddieProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.net.ProxySelector;
import java.net.http.HttpClient;
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

    // ==================== Agent 库（独立数据库 eddie-agent.db） ====================

    @Bean(name = "agentDataSource")
    public DataSource agentDataSource(EddieProperties props, ResourceLoader resourceLoader) {
        var agentDs = props.getAgentDatasource();
        var config = new HikariConfig();
        config.setJdbcUrl(agentDs.getUrl());
        config.setDriverClassName(agentDs.getDriverClassName());
        config.setMaximumPoolSize(agentDs.getMaximumPoolSize());
        var ds = new HikariDataSource(config);

        // 同步执行 DDL（在 DataSource 创建后立即执行），避免 InitializingBean 回调在 AOT 下失效
        if ("always".equals(agentDs.getInitMode())) {
            var populator = new ResourceDatabasePopulator();
            populator.addScript(resourceLoader.getResource(agentDs.getSchemaLocation()));
            DatabasePopulatorUtils.execute(populator, ds);
            log.info("Agent 数据库 DDL 已执行: {}", agentDs.getSchemaLocation());
        } else {
            log.info("Agent 数据库 DDL 已跳过 (init-mode={})", agentDs.getInitMode());
        }
        return ds;
    }

    @Bean(name = "agentJdbcTemplate")
    public JdbcTemplate agentJdbcTemplate(@Autowired @Qualifier(value = "agentDataSource") DataSource agentDataSource) {
        return new JdbcTemplate(agentDataSource);
    }

    @Bean(name = "agentTransactionManager")
    public PlatformTransactionManager agentTransactionManager(
            @Autowired @Qualifier("agentDataSource") DataSource agentDataSource) {
        return new DataSourceTransactionManager(agentDataSource);
    }

    @Bean(name = "agentTransactionTemplate")
    public TransactionTemplate agentTransactionTemplate(
            @Autowired @Qualifier("agentTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    // ==================== 通用 Bean ====================

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 创建 RestClient，通过 {@code java.net.useSystemProxies} 启用系统原生代理检测。
     * <p>
     * 跨平台支持 macOS/Windows/Linux 系统代理设置：
     * <ul>
     *   <li>macOS：读取 System Configuration（scutil），兼容 Clash/Surge/Shadowsocks 等</li>
     *   <li>Windows：读取 Internet Options 注册表代理设置</li>
     *   <li>Linux：读取 GNOME 设置或环境变量</li>
     * </ul>
     * TUN 模式（虚拟网卡接管）无需代理配置，流量透明通过。
     * 环境变量 {@code HTTPS_PROXY} / {@code http_proxy} / {@code ALL_PROXY} 优先级高于系统代理。
     */
    @Bean
    public RestClient restClient() {
        // 启用 JDK 系统代理检测（读取 OS 原生代理设置）
        System.setProperty("java.net.useSystemProxies", "true");

        HttpClient httpClient = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
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
