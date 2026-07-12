/**
 * @author Eddie
 * {@code @date} 2026-07-12
 */

package cc.wlizhi.eddie.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LazyNonProjectBeansProcessor implements BeanFactoryPostProcessor {

    private static final String PROJECT_PACKAGE = "cc.wlizhi.eddie";

    /**
     * 必须保持 Eager 的基础设施包前缀列表。
     * 这些框架核心组件启动时就必须创建，否则服务器无法启动。
     */
    private static final List<String> EAGER_PACKAGES = List.of(
            PROJECT_PACKAGE,
            "org.springframework.boot.web",
            "org.springframework.web",
            "org.apache.catalina",
            "org.apache.tomcat"
    );

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        var lazyBeans = new ArrayList<String>();

        for (String beanName : factory.getBeanDefinitionNames()) {
            var bd = factory.getBeanDefinition(beanName);
            if (!(bd instanceof AbstractBeanDefinition abd)) {
                continue;
            }
            if (isEager(abd)) {
                continue;
            }
            abd.setLazyInit(true);
            lazyBeans.add(beanName);
        }

        if (!lazyBeans.isEmpty()) {
            log.info("已标记 {} 个非项目 Bean 为懒加载（首使用时初始化）", lazyBeans.size());
            if (log.isDebugEnabled()) {
                lazyBeans.forEach(name -> log.debug("  -> @Lazy: {}", name));
            }
        }
    }

    private boolean isEager(AbstractBeanDefinition abd) {
        // 检查 Bean 类名是否属于 EAGER_PACKAGES
        String className = abd.getBeanClassName();
        if (className != null && belongsToPackages(className)) {
            return true;
        }

        // 工厂方法 Bean：检查工厂 Bean 的类名
        String factoryName = abd.getFactoryBeanName();
        if (factoryName != null) {
            abd.getResolvableType();
            Class<?> resolved = abd.getResolvableType().resolve();
            if (resolved != null) {
                String resolvedName = resolved.getName();
                return belongsToPackages(resolvedName);
            }
        }

        return false;
    }

    private boolean belongsToPackages(String className) {
        for (String pkg : EAGER_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
