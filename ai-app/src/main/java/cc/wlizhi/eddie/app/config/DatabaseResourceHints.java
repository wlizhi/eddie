package cc.wlizhi.eddie.app.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class DatabaseResourceHints implements RuntimeHintsRegistrar {
    // 1. 实现 RuntimeHintsRegistrar 接口
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 这里直接注册你的资源路径（支持 Ant 风格通配符）
        hints.resources().registerPattern("init/*.sql");
        // 如果你只想包含特定文件，也可以精确指定：
        // hints.resources().registerPattern("db/init/xx.sql");
    }
}
