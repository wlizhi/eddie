package cc.wlizhi.eddie.app;

import cc.wlizhi.eddie.app.config.DatabaseResourceHints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.event.EventListener;

import java.nio.file.Files;
import java.nio.file.Path;

@ImportRuntimeHints(DatabaseResourceHints.class)
@SpringBootApplication(
        scanBasePackages = "cc.wlizhi.eddie"
)
public class EddieAiApplication {

    public EddieAiApplication(@Value("${eddie.data-dir}") String dataDir) {
        try {
            Files.createDirectories(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("无法创建数据目录: " + dataDir, e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(EddieAiApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser(ApplicationReadyEvent event) {
        Integer port = event.getApplicationContext().getEnvironment().getProperty("server.port", Integer.class);
        String url = "http://localhost:" + port;
        try {
            Runtime.getRuntime().exec(new String[]{"open", url});
        } catch (Exception ignored) {
        }
    }
}
