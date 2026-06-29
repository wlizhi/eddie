package cc.wlizhi.eddie.app.init;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.util.FileStorageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储初始化配置
 * <p>
 * 从 application.yml 读取 eddie.data-dir 配置，
 * 初始化 {@link FileStorageUtil} 的存储目录。
 */
@Configuration
public class FileStorageInitializer {
    @Resource
    private InitScheduler initScheduler;

    @Value("${eddie.data-dir}")
    private String dataDir;

    @PostConstruct
    public void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 0, () -> FileStorageUtil.init(dataDir));
    }
}
