package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 多服务商多模型聊天客户端工厂
 * <p>
 * 职责单一：只负责判断是否支持某服务商，以及从 {@link ChatContext} 中读取配置构建对应的 ChatClient。
 * 事件提取（thinking / metadata）交由 ThinkingHandler / MetadataHandler 处理。
 */
public interface ChatClientFactory {

    boolean support(String providerCode);

    ChatClient getChatClient(ChatContext ctx);
}
