package cc.wlizhi.eddieai.settings.controller;

import cc.wlizhi.eddieai.common.dto.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局设置 API（骨架）
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @GetMapping("/hello")
    public ApiResult<String> hello() {
        return ApiResult.success("Hello from Settings");
    }
}
