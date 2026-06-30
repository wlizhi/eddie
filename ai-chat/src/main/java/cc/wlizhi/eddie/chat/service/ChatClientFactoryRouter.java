/**
 * ChatClientFactoryRouter — 聊天客户端工厂路由器
 * <p>
 * 根据服务商代码（providerCode）从已注册的 ChatClientFactory 列表中
 * 匹配对应的工厂实现。Agent 模块也可注入此路由器来复用工厂路由。
 * <p>
 * 职责：
 * - 遍历 List&lt;ChatClientFactory&gt; 按 support() 匹配
 * - 未匹配到任何工厂时返回默认工厂（defaultFactory）
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-22
 */

package cc.wlizhi.eddie.chat.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatClientFactoryRouter {

    @Resource
    private List<ChatClientFactory> factories;

    @Resource
    private ChatClientFactory defaultChatClientFactory;

    /**
     * 根据服务商代码解析对应的聊天客户端工厂
     *
     * @param providerCode 服务商代码（如 openai / deepseek）
     * @return 匹配的 ChatClientFactory，无匹配时返回 defaultChatClientFactory
     */
    public ChatClientFactory resolve(String providerCode) {
        return factories.stream()
                .filter(f -> f.support(providerCode))
                .findFirst()
                .orElse(defaultChatClientFactory);
    }
}
