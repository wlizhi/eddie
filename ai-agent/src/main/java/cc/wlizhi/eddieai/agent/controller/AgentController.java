package cc.wlizhi.eddieai.agent.controller;

import cc.wlizhi.eddieai.common.dto.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体 API（骨架）
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @GetMapping("/hello")
    public ApiResult<String> hello() {
        return ApiResult.success("Hello from Agent");
    }
}
