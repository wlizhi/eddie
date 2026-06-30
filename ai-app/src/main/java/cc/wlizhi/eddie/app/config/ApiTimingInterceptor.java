package cc.wlizhi.eddie.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 请求耗时统计拦截器
 * <p>
 * 统计所有 /api/** 请求的 HTTP 全链路耗时（含序列化、网络 IO），
 * 输出到日志，并在响应头中返回 X-Response-Time-Ms。
 * @author Eddie
 * {@code @date} 2026-06-30
 */
@Component
public class ApiTimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiTimingInterceptor.class);
    private static final String START_TIME_ATTR = "API_START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) return;

        long durationMs = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        log.info("[API] {} {} {} => {}ms", method, path, status, durationMs);

        // 非 SSE 请求在响应头返回耗时信息
        String contentType = response.getContentType();
        if (contentType == null || !contentType.contains("text/event-stream")) {
            response.setHeader("X-Response-Time-Ms", String.valueOf(durationMs));
        }
    }
}
