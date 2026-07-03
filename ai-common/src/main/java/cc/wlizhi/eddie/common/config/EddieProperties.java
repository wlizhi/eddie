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

    private String version;
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
}
