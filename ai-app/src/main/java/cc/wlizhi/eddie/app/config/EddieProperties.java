package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.app.init.DatabaseDataInitializer;
import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.memory.context.BuiltInPrompts;
import cc.wlizhi.eddie.memory.context.GlobalPromptsContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库初始化脚本配置。
 * <p>从 {@code application.yml} 的 {@code eddie.db.init-scripts} 读取，
 * 由 {@link DatabaseDataInitializer} 在运行时按精确路径加载。
 *
 * @author Eddie
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eddie")
public class EddieProperties {
    @Resource
    private InitScheduler initScheduler;

    /**
     * 数据库初始化 SQL 脚本 classpath 路径列表
     */
    private List<String> initScripts = new ArrayList<>();

    /**
     * 提示词模板文件映射
     */
    @NestedConfigurationProperty
    private BuiltInPrompts prompts;

    @Resource
    private GlobalPromptsContext globalPromptsContext;

    @PostConstruct
    void init() {
        initScheduler.addTask(GlobalPromptsContext.class.getSimpleName(), 10, () -> globalPromptsContext.init(prompts));
    }
}
