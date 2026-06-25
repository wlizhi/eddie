package cc.wlizhi.eddie.tools.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.tools.tool.WebFetchTools;
import cc.wlizhi.eddie.tools.tool.WebSearchTools;
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

    @Resource
    private WebSearchTools webSearchTools;

    @GetMapping("/hello")
    public ApiResult<String> hello(@RequestParam("query") String query) {
        String content = webSearchTools.search(query, 10);
        return ApiResult.success(content);
    }
}
