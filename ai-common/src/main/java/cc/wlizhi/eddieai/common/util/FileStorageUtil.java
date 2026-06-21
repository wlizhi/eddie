package cc.wlizhi.eddieai.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 文件存储工具类（纯静态，无需 Spring 依赖）
 * <p>
 * 由 ai-app 模块启动时调用 {@link #init(String)} 初始化，
 * 后续各业务模块（ai-chat、ai-agent 等）可直接调用 save/delete 方法。
 * <p>
 * 文件统一存储在 {dataDir}/images/ 目录下，
 * 访问路径为 /api/files/{uuid}.{ext}
 */
public class FileStorageUtil {

    private static Path imagesDir;
    private static boolean initialized = false;

    /**
     * 初始化存储目录（由 Spring 启动时调用一次）
     *
     * @param dataDir 数据根目录，如 ${user.home}/.eddie-ai
     */
    public static void init(String dataDir) {
        imagesDir = Path.of(dataDir, "images");
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建图片存储目录: " + imagesDir, e);
        }
        initialized = true;
    }

    /**
     * 保存文件到 images 目录
     *
     * @param data 文件二进制数据
     * @param ext  文件扩展名（不含点），如 "webp"、"png"
     * @return 可访问的 URL 路径，如 /api/files/uuid.webp
     */
    public static String save(byte[] data, String ext) {
        checkInit();
        try {
            String filename = UUID.randomUUID() + "." + ext;
            Files.write(imagesDir.resolve(filename), data);
            return "/api/files/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }
    }

    /**
     * 根据 URL 删除文件
     *
     * @param url 文件 URL，如 /api/files/uuid.webp
     */
    public static void delete(String url) {
        checkInit();
        try {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Files.deleteIfExists(imagesDir.resolve(filename));
        } catch (IOException e) {
            // 删除文件失败不影响主流程，仅打印警告
            System.err.println("警告：删除文件失败 - " + url);
        }
    }

    /**
     * 判断 URL 是否为本站存储的图片文件
     */
    public static boolean isFileUrl(String url) {
        return url != null && url.startsWith("/api/files/");
    }

    private static void checkInit() {
        if (!initialized) {
            throw new IllegalStateException("FileStorageUtil 未初始化，请先调用 init()");
        }
    }
}
