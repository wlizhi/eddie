package cc.wlizhi.eddie.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库初始化脚本配置。
 * <p>从 {@code application.yml} 的 {@code eddie.db.init-scripts} 读取，
 * 由 {@link DatabaseInitExecutor} 在运行时按精确路径加载。
 *
 * @author Eddie
 */
@Getter
@Setter
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "eddie.db")
public class DatabaseInitProperties {

    /**
     * 数据库初始化 SQL 脚本 classpath 路径列表
     */
    private List<String> initScripts = new ArrayList<>();
}
