package cc.wlizhi.eddieai.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * 前端 SPA 路由支持：非 /api 路径刷新时转发到 index.html
 * <p>
 * 前端使用 Vue Router 的 HTML5 History 模式（createWebHistory），
 * 路由由前端 JS 控制。直接访问 /chat、/settings 等路径时，
 * Spring Boot 需要返回 index.html 让前端路由接管。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @jakarta.annotation.Resource
    private ApiTimingInterceptor apiTimingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiTimingInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * 自定义资源解析器：请求的资源不存在时，如果是非 /api 路径，
     * 则返回 index.html 交给前端路由处理。
     */
    private static class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) {
            try {
                // 先尝试正常解析资源
                Resource resource = location.createRelative(resourcePath);
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
                // 资源不存在且不是 api 请求 → 返回 index.html
                if (!resourcePath.startsWith("api/")) {
                    Resource index = location.createRelative("index.html");
                    if (index.exists()) {
                        return index;
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
}
