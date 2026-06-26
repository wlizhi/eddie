package cc.wlizhi.eddie.settings.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.settings.service.GlobalConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 全局设置 API
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Resource
    private GlobalConfigService globalConfigService;

    /**
     * 获取全部全局配置。<p>
     * 返回 {@code Map<configKey, configVal>}，每个 value 是独立 JSON 字符串，前端自行解析渲染。
     */
    @GetMapping("/configs")
    public ApiResult<Map<String, String>> getConfigs() {
        return ApiResult.success(globalConfigService.getConfigs());
    }

    /**
     * 全量更新全局配置。<p>
     * 请求体为 {@code Map<configKey, configVal>}，仅更新 enum 中已定义的 key，非法 key 自动忽略。<br>
     * 修改后自动刷新缓存。
     */
    @PutMapping("/configs")
    public ApiResult<Void> updateConfigs(@RequestBody Map<String, String> configs) {
        globalConfigService.updateConfigs(configs);
        return ApiResult.success();
    }

    /**
     * 更新用户头像（支持文字、emoji、图片上传）。
     * <p>
     * 内部从 DISPLAY_SETTINGS 配置中读取当前值，替换 avatar 字段后写回。
     *
     * @param avatarText 文字或 emoji（可选）
     * @param file       图片文件（可选）
     * @return 更新后的头像值
     */
    @PostMapping("/user-avatar")
    public ApiResult<String> updateUserAvatar(
            @RequestParam(value = "avatar", required = false) String avatarText,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String newAvatar = globalConfigService.updateUserAvatar(avatarText, file);
        return ApiResult.success(newAvatar);
    }
}
