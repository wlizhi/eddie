package cc.wlizhi.eddieai.settings.controller;

import cc.wlizhi.eddieai.common.dto.ApiResult;
import cc.wlizhi.eddieai.settings.service.GlobalConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
}
