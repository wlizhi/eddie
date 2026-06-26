package cc.wlizhi.eddie.tools.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.tools.tool.WebFetchTools;
import cc.wlizhi.eddie.tools.tool.WebSearchTools;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/search")
    public ApiResult<String> search(@RequestParam("query") String query
            , @RequestParam(required = false, name = "maxResults") Integer maxResults) {
        String result = webSearchTools.search(query, maxResults);
        return ApiResult.success(result);
    }

    @GetMapping("/fetchMarkdown")
    public ApiResult<String> fetchMarkdown(@RequestParam("url") String url
            , @RequestParam(required = false, name = "maxCharacters") Integer maxCharacters
            , @RequestParam(required = false, name = "mode") String mode
    ) {
        String result = webFetchTools.fetchMarkdown(List.of(url), maxCharacters, mode);
        return ApiResult.success(result);
    }
}
