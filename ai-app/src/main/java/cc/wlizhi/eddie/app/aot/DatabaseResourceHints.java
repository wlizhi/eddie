/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.app.aot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class DatabaseResourceHints implements RuntimeHintsRegistrar {
    // 1. 实现 RuntimeHintsRegistrar 接口
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 这里直接注册你的资源路径（支持 Ant 风格通配符）
        hints.resources().registerPattern("db/ddl/*.sql");
        hints.resources().registerPattern("db/init/*.sql");
        hints.resources().registerPattern("prompts/*.md");
    }
}
