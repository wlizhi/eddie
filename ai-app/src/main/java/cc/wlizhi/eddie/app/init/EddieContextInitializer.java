package cc.wlizhi.eddie.app.init;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 初始化数据目录
 *
 * @author eddie
 */
@Slf4j
public class EddieContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        try {
            String dataDir = applicationContext.getEnvironment().getProperty("eddie.data-dir");
            if (dataDir == null) {
                dataDir = System.getProperty("user.home") + "/.eddie";
            }
            log.info("初始化数据存放目录: {}", dataDir);
            Files.createDirectories(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("无法创建数据目录: ");
        }
    }
}
