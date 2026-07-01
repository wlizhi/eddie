/**
 * @author Eddie
 * {@code @date} 2026-07-01
 */

package cc.wlizhi.eddie.chat.config;

import cc.wlizhi.eddie.common.cache.EventRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天模块事件注册表配置
 */
@Configuration
public class ChatEventRegistryConfig {

    @Bean
    public EventRegistry chatEventRegistry() {
        return new EventRegistry();
    }
}
