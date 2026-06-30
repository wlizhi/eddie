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
