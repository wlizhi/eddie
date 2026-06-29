package cc.wlizhi.eddie.settings.service.impl;

import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.util.FileStorageUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.dao.GlobalConfigDao;
import cc.wlizhi.eddie.settings.service.GlobalConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局配置业务实现
 *
 * @author Eddie
 */
@Service
public class GlobalConfigServiceImpl implements GlobalConfigService {

    private static final Logger log = LoggerFactory.getLogger(GlobalConfigServiceImpl.class);

    private static final String DISPLAY_SETTINGS_KEY = GlobalConfigKey.DISPLAY_SETTINGS.name();

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private GlobalConfigDao globalConfigDao;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, String> getConfigs() {
        return globalConfigContext.getAllConfigs();
    }

    @Override
    public void updateConfigs(Map<String, String> configs) {
        // 过滤非法 key：只保留 GlobalConfigKey 枚举中定义的 key
        Map<String, String> validConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            if (isValidKey(key)) {
                validConfigs.put(key, entry.getValue());
            } else {
                log.warn("忽略非法的全局配置 key: {}", key);
            }
        }

        // 写入 DB（DELETE + batch INSERT，单事务）
        globalConfigDao.replaceAll(validConfigs);

        // 刷新缓存
        globalConfigContext.refresh();
    }

    @Override
    public String updateUserAvatar(String avatarText, MultipartFile file) {
        // 1. 计算新头像值
        String newAvatar;
        if (file != null && !file.isEmpty()) {
            // 上传图片 → 保存到磁盘
            try {
                byte[] data = file.getBytes();
                String ext = "webp";
                String originalName = file.getOriginalFilename();
                if (originalName != null && originalName.contains(".")) {
                    String origExt = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
                    if (origExt.equals("png") || origExt.equals("jpg") || origExt.equals("jpeg")
                            || origExt.equals("gif") || origExt.equals("webp")) {
                        ext = origExt;
                    }
                }
                newAvatar = FileStorageUtil.save(data, ext);
            } catch (IOException e) {
                throw new RuntimeException("读取上传文件失败", e);
            }
        } else if (avatarText != null && !avatarText.isEmpty()) {
            newAvatar = avatarText;
        } else {
            throw new IllegalArgumentException("请提供头像文字或图片");
        }

        // 2. 获取当前 DISPLAY_SETTINGS 配置
        Map<String, Object> displaySettings = getDisplaySettingsMap();

        // 3. 旧头像如果是图片路径 → 删除旧文件
        String oldAvatar = (String) displaySettings.get("avatar");
        if (oldAvatar != null && FileStorageUtil.isFileUrl(oldAvatar)) {
            FileStorageUtil.delete(oldAvatar);
        }

        // 4. 替换 avatar 字段
        displaySettings.put("avatar", newAvatar);

        // 5. 写回 DB
        try {
            String updatedJson = objectMapper.writeValueAsString(displaySettings);
            Map<String, String> configs = new LinkedHashMap<>();
            configs.put(DISPLAY_SETTINGS_KEY, updatedJson);
            globalConfigDao.replaceAll(configs);
            globalConfigContext.refresh();
        } catch (IOException e) {
            throw new RuntimeException("序列化显示设置失败", e);
        }

        log.info("用户头像已更新: {}", newAvatar);
        return newAvatar;
    }

    /**
     * 获取当前 DISPLAY_SETTINGS 的 Map 表示
     */
    private Map<String, Object> getDisplaySettingsMap() {
        String json = globalConfigContext.getConfig(GlobalConfigKey.DISPLAY_SETTINGS);
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            log.warn("解析 DISPLAY_SETTINGS 失败，将使用空配置: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * 判断 key 是否在 GlobalConfigKey 枚举中定义
     */
    private boolean isValidKey(String key) {
        if (key == null) {
            return false;
        }
        for (GlobalConfigKey enumKey : GlobalConfigKey.values()) {
            if (enumKey.name().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
