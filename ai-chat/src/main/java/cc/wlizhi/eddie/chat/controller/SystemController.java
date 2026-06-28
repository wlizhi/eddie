package cc.wlizhi.eddie.chat.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.util.PromptVariableResolver;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统信息接口
 * <p>
 * 提供系统级数据查询接口，如系统提示词模板变量列表。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Resource
    private PromptVariableResolver promptVariableResolver;

    /**
     * 获取系统提示词支持的模板变量列表
     * <p>
     * 前端可据此渲染悬浮提示或展开面板，方便用户查看、复制变量名。
     * 后期在 {@link PromptVariableResolver#init()} 中新增变量时，
     * 此接口自动返回新变量，前端无需改动代码。
     *
     * @return 变量信息列表（key / template / example / description）
     */
    @GetMapping("/prompt-variables")
    public ApiResult<List<PromptVariableResolver.VariableInfo>> getPromptVariables() {
        return ApiResult.success(promptVariableResolver.getSupportedVariables());
    }
}
