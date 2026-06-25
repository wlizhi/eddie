package cc.wlizhi.eddie.role.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.role.tool.WebFetchTools;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色 API（骨架）
 */
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Resource
    private WebFetchTools webFetchTools;

    @GetMapping("/hello")
    public ApiResult<String> hello(@RequestParam("url") String url) {
        String content = webFetchTools.fetchJson(url);
        return ApiResult.success(content);
    }
}
