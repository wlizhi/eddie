/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.agent.service;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 多服务商多模型聊天客户端工厂
 * <p>
 * 职责单一：只负责判断是否支持某服务商，以及从 {@link ChatContext} 中读取配置构建对应的 ChatClient。
 * 事件提取（thinking / metadata）交由 ThinkingHandler / MetadataHandler 处理。
 */
public interface AgentChatClientFactory {

    boolean support(String providerCode);

    ChatClient getChatClient(AgentChatContext ctx);
}
