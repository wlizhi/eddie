package cc.wlizhi.eddie.app.config;

import cc.wlizhi.eddie.common.util.FileStorageUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储初始化配置
 * <p>
 * 从 application.yml 读取 eddie.data-dir 配置，
 * 初始化 {@link FileStorageUtil} 的存储目录。
 */
@Configuration
public class FileStorageConfig {

    @Value("${eddie.data-dir}")
    private String dataDir;

    @PostConstruct
    public void init() {
        FileStorageUtil.init(dataDir);
    }
}
