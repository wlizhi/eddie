/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.app.controller;

import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 * <p>
 * 用于 Electron 检测后端是否就绪，不依赖任何业务数据。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /**
     * 健康检查
     *
     * @return 始终返回成功
     */
    @GetMapping
    public ApiResult<String> health(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("/api/health called from: remote={}, user-agent={}",
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
        return ApiResult.success("ok");
    }
}
