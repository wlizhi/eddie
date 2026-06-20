/**
 * ChatPolicyRouter — 聊天策略路由器
 * <p>
 * 根据服务商代码（providerCode）从已注册的 ChatPolicy 列表中
 * 匹配对应的策略实现。Agent 模块也可注入此路由器来复用策略路由。
 * <p>
 * 职责：
 * - 遍历 List<ChatPolicy> 按 support() 匹配
 * - 未匹配到任何策略时返回默认策略（defaultPolicy）
 */
package cc.wlizhi.eddieai.chat.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatPolicyRouter {

    @Resource
    private List<ChatPolicy> policies;

    @Resource
    private ChatPolicy defaultChatPolicy;

    /**
     * 根据服务商代码解析对应的聊天策略
     *
     * @param providerCode 服务商代码（如 openai / deepseek）
     * @return 匹配的 ChatPolicy，无匹配时返回 defaultChatPolicy
     */
    public ChatPolicy resolve(String providerCode) {
        return policies.stream()
                .filter(p -> p.support(providerCode))
                .findFirst()
                .orElse(defaultChatPolicy);
    }
}
