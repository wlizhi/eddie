/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenBrowserInitializer {
    @Resource
    private InitScheduler initScheduler;
    @Resource
    private Environment environment;

    @PostConstruct
    public void openBrowser() {
        // Electron 模式下由 Electron 管理窗口，跳过自动打开浏览器
        if ("true".equalsIgnoreCase(System.getenv("EDDIE_ELECTRON"))) {
            return;
        }

        initScheduler.addTask(this.getClass().getSimpleName(), 1000000, () -> {
            Integer port = environment.getProperty("server.port", Integer.class);
            String url = "http://localhost:" + port;
            try {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } catch (Exception ignored) {
            }
        });
    }
}
