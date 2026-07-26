package cc.wlizhi.eddie.common.config;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eddie")
public class EddieProperties {

    /**
     * 软件版本
     */
    private String version;
    @Resource
    private InitScheduler initScheduler;

    /**
     * 数据库初始化 SQL 脚本 classpath 路径列表
     */
    private List<String> initScripts = new ArrayList<>();

    /**
     * 数据库 DDL 迁移 SQL 脚本 classpath 路径列表
     * 与 initScripts 共用 DB_INIT_VERSION 版本号，迁移脚本执行完后再执行初始化脚本。
     * 文件名规则与 initScripts 一致：末尾 _数字.sql / -数字.sql 中的数字即为版本号。
     */
    private List<String> migrationScripts = new ArrayList<>();

    /**
     * Agent 数据库 DDL 迁移 SQL 脚本 classpath 路径列表
     * 文件名规则同 migrationScripts，在 Agent 库各自的 global_config 中独立维护版本号。
     */
    private List<String> agentMigrationScripts = new ArrayList<>();

    /**
     * Agent 数据库初始化 SQL 脚本 classpath 路径列表
     * 文件名规则同 initScripts，在 Agent 库各自的 global_config 中独立维护版本号。
     */
    private List<String> agentInitScripts = new ArrayList<>();

    /**
     * 提示词模板文件映射
     */
    @NestedConfigurationProperty
    private BuiltInPrompts prompts;

    /**
     * Agent 独立数据库连接配置
     */
    @NestedConfigurationProperty
    private AgentDatasource agentDatasource = new AgentDatasource();

    @Getter
    @Setter
    public static class AgentDatasource {
        private String url;
        private String driverClassName;
        private int maximumPoolSize = 1;
        private String schemaLocation;
        /** DDL 初始化模式：always=每次都执行, never=禁用 */
        private String initMode = "always";
    }
}
