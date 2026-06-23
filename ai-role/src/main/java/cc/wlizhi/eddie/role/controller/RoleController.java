package cc.wlizhi.eddie.role.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色 API（骨架）
 */
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @GetMapping("/hello")
    public ApiResult<String> hello() {
        return ApiResult.success("Hello from Role");
    }
}
