package cc.wlizhi.eddieai.app;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.File;

@SpringBootApplication(
        scanBasePackages = "cc.wlizhi.eddieai"
)
public class EddieAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EddieAiApplication.class, args);
    }

    @PostConstruct
    public void init() {
        String path = System.getProperty("user.home") + "/.eddie-ai";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
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
