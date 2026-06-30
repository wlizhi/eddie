package cc.wlizhi.eddie.app;

import cc.wlizhi.eddie.app.aot.DatabaseResourceHints;
import cc.wlizhi.eddie.app.aot.EddieReflectionHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints({DatabaseResourceHints.class, EddieReflectionHints.class})
@SpringBootApplication(
        scanBasePackages = "cc.wlizhi.eddie"
)
public class EddieAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EddieAiApplication.class, args);
    }
}
