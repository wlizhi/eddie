package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 全局配置业务接口
 *
 * @author Eddie
 */
public interface GlobalConfigService {

    /**
     * 获取全部全局配置
     */
    Map<String, String> getConfigs();

    /**
     * 全量更新全局配置。<p>
     * 会过滤掉不在 {@link GlobalConfigKey} 中的非法 key。<br>
     * 更新后自动刷新缓存。
     */
    void updateConfigs(Map<String, String> configs);

    /**
     * 更新用户头像（支持文字、emoji、图片上传）。
     * <p>
     * 内部从 DISPLAY_SETTINGS 配置中读取当前值，替换 avatar 字段后写回。
     *
     * @param avatarText 文字或 emoji（可选）
     * @param file       图片文件（可选）
     * @return 更新后的头像值（文字/emoji 或图片 URL）
     */
    String updateUserAvatar(String avatarText, MultipartFile file);
}
