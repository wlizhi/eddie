package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 多服务商多模型聊天策略
 * <p>
 * 职责单一：只负责判断是否支持某服务商，以及构建对应的 ChatClient。
 * 事件提取（thinking / metadata）交由 ThinkingHandler / MetadataHandler 处理。
 */
public interface ChatPolicy {

    boolean support(String providerCode);

    ChatClient getChatClient(ChatClientGetDTO chatClientGetDTO);
}
