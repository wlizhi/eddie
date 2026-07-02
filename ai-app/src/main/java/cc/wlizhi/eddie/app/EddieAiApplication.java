/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.app;

import cc.wlizhi.eddie.app.aot.DatabaseResourceHints;
import cc.wlizhi.eddie.app.aot.EddieReflectionHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@ImportRuntimeHints({DatabaseResourceHints.class, EddieReflectionHints.class})
@SpringBootApplication(
        scanBasePackages = "cc.wlizhi.eddie"
)
public class EddieAiApplication {

    public static void main(String[] args) {
        initPath();
        SpringApplication.run(EddieAiApplication.class, args);
    }

    public static void initPath() {
        try {
            var eddieHome = Path.of(System.getProperty("user.home"), ".eddie");
            var data = eddieHome.resolve("data");
            var image = eddieHome.resolve("images");
            var logs = eddieHome.resolve("logs");
            System.setProperty("eddie.home", eddieHome.toString());
            System.setProperty("eddie.data", data.toString());
            System.setProperty("eddie.images", image.toString());
            System.setProperty("eddie.logs", logs.toString());
            log.info("初始化 eddieHome 目录: {}", eddieHome);
            log.info("初始化数据存放目录: {}", data);
            log.info("初始化图片存放目录: {}", image);
            log.info("初始化日志存放目录: {}", logs);
            Files.createDirectories(eddieHome);
            Files.createDirectories(data);
            Files.createDirectories(image);
            Files.createDirectories(logs);
        } catch (Exception e) {
            throw new RuntimeException("无法创建数据目录: ");
        }
    }
}
